package pl.pabilo8.immersiveintelligence.client.fx.particles;

import blusunrize.immersiveengineering.common.util.chickenbones.Matrix4;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.pabilo8.immersiveintelligence.client.fx.factories.ParticleFactory;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleDrawStages;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleProgram;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleProperties;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleSystem;
import pl.pabilo8.immersiveintelligence.common.util.easynbt.EasyNBT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Pabilo8
 * @updated 05.04.2024
 * @ii-approved 0.3.1
 * @since 23.12.2020
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractParticle implements INBTSerializable<NBTTagCompound>
{
	//--- Constants ---//
	private static final Vector2f ZERO = new Vector2f(0, 0);
	private static final Set<ParticleProgram> DEFAULT_PROGRAMS = Collections.emptySet();

	//--- Static Fields ---//
	/**
	 * Camera view position for current draw frame.<br>
	 * Updated by {@link ParticleSystem}
	 */
	public static Vec3d interPos;
	/**
	 * Camera view direction for current draw frame.<br>
	 * Updated by {@link ParticleSystem}
	 */
	public static Vec3d cameraViewDir;
	public static float interpTicks;

	//--- Instance Fields ---//
	/**
	 * the world this particle is in
	 */
	protected World world;
	/**
	 * previous tick position of this particle
	 */
	protected Vector3f prevPos;
	/**
	 * current position of the particle
	 */
	protected Vector3f pos;
	/**
	 * motion of the particle
	 */
	protected Vector3f motion;
	/**
	 * rotation of the particle
	 */
	protected double rotationYaw, rotationPitch;
	/**
	 * transformation matrix for this particle
	 */
	protected Matrix4 matrix = new Matrix4();
	//--- Properties ---//
	/**
	 * how many ticks should the particle be displayed for
	 */
	protected int lifeTime, maxLifeTime;
	/**
	 * whether the particle is on ground
	 */
	protected boolean onGround;
	/**
	 * this particle's bounding box without offsets
	 */
	private AxisAlignedBB baseBoundingBox;
	/**
	 * the collision / bounding box of this particle
	 */
	protected AxisAlignedBB boundingBox;

	//--- Programmable Particles ---//
	/**
	 * Programs executed by the particle
	 */
	@Nonnull
	protected Set<ParticleProgram> programs = DEFAULT_PROGRAMS;

	//--- Chaining Fields ---//
	private Multimap<Integer, Consumer<? super AbstractParticle>> scheduledParticles;
	private Set<Consumer<? super AbstractParticle>> chainedParticles;

	//--- Drawing Related Fields ---//
	/**
	 * at which stage of the rendering process should this particle be drawn
	 */
	protected ParticleDrawStages drawStage;

	/**
	 * Extending constructors should be passed as a parameter to {@link ParticleFactory}
	 */
	public AbstractParticle(World world, Vec3d pos)
	{
		this.world = world;
		this.pos = new Vector3f((float)pos.x, (float)pos.y, (float)pos.z);
		this.prevPos = new Vector3f((float)pos.x, (float)pos.y, (float)pos.z);
		this.motion = new Vector3f(0, 0, 0);
		this.rotationYaw = 0;
		this.rotationPitch = 0;
		this.baseBoundingBox = null;
		this.boundingBox = null;
	}

	//--- Handling Methods ---//

	/**
	 * Called every tick to update the particle's position and motion
	 */
	public void onUpdate()
	{
		if(isAlive())
		{
			if(lifeTime==0)
				programs.forEach(p -> p.onParticleCreation(this));

			prevPos.set(pos);
			programs.forEach(p -> p.onParticleMovement(this));
			move();

			if(scheduledParticles!=null)
				for(Consumer<? super AbstractParticle> consumer : scheduledParticles.get(this.lifeTime))
					consumer.accept(this);

			this.lifeTime++;
		}
	}

	/**
	 * Performs movement of the particle using the motion vector
	 */
	private void move()
	{
		if(motion.lengthSquared()==0)
			return;

		//check for collisions against other bounding boxes in motion area
		if(this.boundingBox!=null)
		{
			//initial motion values
			Vector3f initMotion = new Vector3f(motion);
			List<AxisAlignedBB> list = this.world.getCollisionBoxes(null, this.boundingBox.expand(motion.x, motion.y, motion.z));

			//if boxes intersect, offset the box, so it doesn't
			for(AxisAlignedBB axisalignedbb : list)
			{
				motion.y = (float)axisalignedbb.calculateYOffset(this.boundingBox, motion.y);
				this.boundingBox = this.baseBoundingBox.offset(pos.x, pos.y+motion.y, pos.z);
				motion.x = (float)axisalignedbb.calculateXOffset(this.boundingBox, motion.x);
				this.boundingBox = this.baseBoundingBox.offset(pos.x+motion.x, pos.y+motion.y, pos.z);
				motion.z = (float)axisalignedbb.calculateZOffset(this.boundingBox, motion.z);
				this.boundingBox = this.baseBoundingBox.offset(pos.x+motion.x, pos.y+motion.y, pos.z+motion.z);
			}
			if(list.isEmpty())
				this.boundingBox = this.baseBoundingBox.offset(pos.x+motion.x, pos.y+motion.y, pos.z+motion.z);

			//check Y axis motion
			this.onGround = initMotion.y!=motion.y&&initMotion.y < 0.0D;

			//stop X and/or Z axis motion if collision was present (partial movement)
			if(initMotion.x!=motion.x)
				this.motion.x = 0f;
			if(initMotion.z!=motion.z)
				this.motion.z = 0f;
		}

		this.pos.add(this.motion);
	}

	//--- Chained and Delayed Particles ---//

	/**
	 * Sets particles that will appear after this one dies
	 *
	 * @param chained particles to be spawned
	 */
	public <T extends AbstractParticle> void setChainedParticles(Set<Consumer<T>> chained)
	{
		//java generics can go to hell
		this.chainedParticles = chained.stream().map(tConsumer -> (Consumer<AbstractParticle>)tConsumer).collect(Collectors.toSet());
	}

	/**
	 * Sets particles that will appear after a delay
	 *
	 * @param delayed map of time in ticks and particles to be spawned
	 */
	public <T extends AbstractParticle> void setScheduledParticles(Multimap<Integer, Consumer<T>> delayed)
	{
		this.scheduledParticles = HashMultimap.create();
		//java generics can go to hell mk.2
		delayed.forEach((integer, tConsumer) ->
				this.scheduledParticles.put(integer, iiParticle -> tConsumer.accept((T)iiParticle))
		);
	}

	//--- Getters and Setters ---//

	/**
	 * @return whether this particle should be displayed
	 */
	public boolean isAlive()
	{
		return this.lifeTime < this.maxLifeTime;
	}

	/**
	 * @param partialTicks partial render ticks
	 * @return lifetime fraction of this particle (0.0 to 1.0)
	 */
	public float getProgress(float partialTicks)
	{
		return MathHelper.clamp((this.lifeTime+partialTicks)/(float)this.maxLifeTime,
				0.0F, 1.0F);
	}

	/**
	 * Sets the position, motion, and rotation of this particle
	 *
	 * @param pos      position of this particle
	 * @param motion   motion of this particle
	 * @param rotation rotation of this particle
	 */
	public final void setPMR(Vec3d pos, Vec3d motion, @Nullable Vector2f rotation)
	{
		this.pos.set((float)pos.x, (float)pos.y, (float)pos.z);
		this.prevPos.set((float)pos.x, (float)pos.y, (float)pos.z);
		this.motion.set((float)motion.x, (float)motion.y, (float)motion.z);

		if(rotation!=null)
		{
			this.rotationYaw = rotation.x;
			this.rotationPitch = rotation.y;
		}
	}

	/**
	 * @param programs programs to be executed by this particle
	 */
	public void setPrograms(Set<ParticleProgram> programs)
	{
		this.programs = programs;
	}

	/**
	 * @return at which stage of the rendering process should this particle be drawn
	 */
	@Nonnull
	public ParticleDrawStages getDrawStage()
	{
		return drawStage;
	}

	//--- NBT Handling ---//

	@Override
	public final NBTTagCompound serializeNBT()
	{
		EasyNBT nbt = EasyNBT.newNBT();
		for(ParticleProperties value : ParticleProperties.values())
		{
			Object property = getProperty(value);
			if(property!=null)
				value.setPropertyToNBT(nbt, property);
		}

		return nbt.unwrap();
	}

	@Override
	public final void deserializeNBT(NBTTagCompound nbt)
	{
		EasyNBT eNBT = EasyNBT.wrapNBT(nbt);
		for(ParticleProperties value : ParticleProperties.values())
			if(eNBT.hasKey(value.getName()))
			{
				Object property = value.getPropertyFromNBT(eNBT);
				if(property!=null)
					setProperty(value, property);
			}
	}

	@Nonnull
	public Object getProperty(ParticleProperties key)
	{
		switch(key)
		{
			case PREVIOUS_POSITION:
				return prevPos;
			case POSITION:
				return pos;
			case MOTION:
				return motion;
			case ROTATION:
				return new Vector2f((float)rotationYaw, (float)rotationPitch);
			case ROTATION_YAW:
				return (float)rotationYaw;
			case ROTATION_PITCH:
				return (float)rotationPitch;

			case LIFETIME:
				return lifeTime;
			case MAX_LIFETIME:
				return maxLifeTime;
			case IS_ALIVE:
				return isAlive();
			case ON_GROUND:
				return onGround;
			case PROGRESS:
				return getProgress(0);

			case DRAW_STAGE:
				return drawStage;
			case BOUNDING_BOX:
				return boundingBox;
		}
		return key.getDefault();
	}

	public final AbstractParticle withProperty(ParticleProperties key, Object value)
	{
		setProperty(key, value);
		return this;
	}

	public void setProperty(ParticleProperties key, Object value)
	{
		switch(key)
		{
			case POSITION:
				this.pos.set((Vector3f)value);
				break;
			case MOTION:
				this.motion.set((Vector3f)value);
				break;
			case ROTATION:
				Vector2f rot = (Vector2f)value;
				this.rotationYaw = rot.x;
				this.rotationPitch = rot.y;
				break;
			case ROTATION_YAW:
				this.rotationYaw = (float)value;
				break;
			case ROTATION_PITCH:
				this.rotationPitch = (float)value;
				break;

			case LIFETIME:
				this.lifeTime = (int)value;
				break;
			case MAX_LIFETIME:
				this.maxLifeTime = (int)value;
				break;
			case PROGRESS:
				this.lifeTime = (int)(this.maxLifeTime*(float)value);
				break;

			case DRAW_STAGE:
				this.drawStage = (ParticleDrawStages)value;
				break;
			case BOUNDING_BOX:
				this.baseBoundingBox = (AxisAlignedBB)value;

		}
	}

	//--- Abstract Methods ---//

	public void preRender(float partialTicks, float x, float xz, float z, float yz, float xy)
	{
		programs.forEach(p -> p.onParticleRender(this, partialTicks));

		matrix.setIdentity()
				.translate(pos.x+motion.x*interpTicks-interPos.x,
						pos.y+motion.y*interpTicks-interPos.y,
						pos.z+motion.z*interpTicks-interPos.z
				)
				.rotate(rotationYaw, 0, 1, 0)
				.rotate(rotationPitch, 1, 0, 0);
	}

	/**
	 * Used to render this particle to the buffer.
	 *
	 * @param buffer       buffer to render to
	 * @param partialTicks partial render ticks
	 * @param x            X rotation angle
	 * @param xz           XZ rotation angle
	 * @param z            Z rotation angle
	 * @param yz           YZ rotation angle
	 * @param xy           XY rotation angle
	 * @implNote Using {@link Tessellator#draw()} directly is not advised (performance issues), but possible, if you do: <pre>{@code
	 * tes.draw();
	 * GlStateManager.pushMatrix();
	 * buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
	 * //...
	 * Tessellator.getInstance().draw();
	 * buffer.begin(GL11.GL_QUADS, format);
	 * }</pre>
	 */
	public abstract void render(BufferBuilder buffer, float partialTicks, float x, float xz, float z, float yz, float xy);

	//--- Utility Methods ---//

	public void onDeath()
	{
		programs.forEach(p -> p.onParticleDeath(this));
		if(chainedParticles!=null)
			this.chainedParticles.forEach(consumer -> consumer.accept(this));
	}

	public World getWorld()
	{
		return world;
	}

}
