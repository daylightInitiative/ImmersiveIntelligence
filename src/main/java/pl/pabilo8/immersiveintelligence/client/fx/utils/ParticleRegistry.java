package pl.pabilo8.immersiveintelligence.client.fx.utils;

import blusunrize.immersiveengineering.client.ClientUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleCloud;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.toposort.TopologicalSort;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.pabilo8.immersiveintelligence.client.fx.factories.ParticleFactory;
import pl.pabilo8.immersiveintelligence.client.fx.particles.AbstractParticle;
import pl.pabilo8.immersiveintelligence.client.render.IReloadableModelContainer;
import pl.pabilo8.immersiveintelligence.client.util.tmt.ModelRendererTurbo;
import pl.pabilo8.immersiveintelligence.common.IILogger;
import pl.pabilo8.immersiveintelligence.common.util.*;
import pl.pabilo8.immersiveintelligence.common.util.easynbt.EasyNBT;

import javax.annotation.Nullable;
import javax.vecmath.Vector2f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A class for registering II particle effects, effect types and programs.
 * Allows loading particle effects from {@link ResLoc#EXT_FX_AMT .fx.amt} (JSON) files.
 *
 * @author Pabilo8
 * @updated 05.04.2024
 * @ii-approved 0.3.1
 * @since 17.07.2020
 */
@SideOnly(Side.CLIENT)
public class ParticleRegistry
{
	//--- Static ---//
	/**
	 * Stores particle factories
	 */
	private static final HashMap<String, ParticleFactory<?>> FACTORIES_REGISTRY = new HashMap<>();
	/**
	 * Stores particle types (factory class constructors)
	 */
	private static final HashMap<String, Supplier<? extends ParticleFactory<?>>> TYPE_REGISTRY = new HashMap<>();
	/**
	 * Stores programs that can be used to modify particles during their lifetime
	 */
	private static final HashMap<String, ParticleProgram> PROGRAMS_REGISTRY = new HashMap<>();
	private static final HashMap<String, ParticleFileEntry> FILELOADER_ENTRIES = new HashMap<>();

	//--- Particle Registry ---//

	/**
	 * Clears the particle registry. Called before assets are reloaded.
	 */
	public static void cleanBuilderRegistry()
	{
		TYPE_REGISTRY.clear();
		FACTORIES_REGISTRY.clear();
		PROGRAMS_REGISTRY.clear();
		FILELOADER_ENTRIES.clear();
	}

	//--- Registry Methods ---//

	/**
	 * Registers a new particle type.
	 *
	 * @param name               The name of the particle
	 * @param factoryConstructor The constructor method of the particle factory
	 */
	public static void registerParticleType(String name, Supplier<? extends ParticleFactory<?>> factoryConstructor)
	{
		TYPE_REGISTRY.put(name, factoryConstructor);
	}

	/**
	 * Registers a new particle effect to be loaded from a file.
	 *
	 * @param fileName The name of the file to load the particle from
	 */
	public static void registerParticle(String fileName)
	{
		FILELOADER_ENTRIES.put(fileName, new ParticleFileEntry(IIReference.RES_II.with(fileName)));
	}

	/**
	 * Retrieves a particle factory from the registry.
	 *
	 * @param particleName name of the particle
	 * @return the particle factory
	 */
	@Nullable
	public static ParticleFactory<? extends AbstractParticle> getParticle(String particleName)
	{
		return FACTORIES_REGISTRY.get(particleName);
	}

	/**
	 * Registers a new {@link ParticleProgram particle program}.
	 *
	 * @param program
	 * @param <T>
	 */
	public static <T extends AbstractParticle> void registerProgram(ParticleProgram program)
	{
		PROGRAMS_REGISTRY.put(program.getProgramName(), program);
	}

	public static ParticleProgram getProgram(String name)
	{
		return PROGRAMS_REGISTRY.get(name);
	}

	//--- Loading ---//

	public static void loadAllParticleFiles()
	{
		TopologicalSort.DirectedGraph<String> graph = new TopologicalSort.DirectedGraph<>();
		FILELOADER_ENTRIES.keySet().forEach(graph::addNode);

		for(String name : FILELOADER_ENTRIES.keySet())
			try
			{
				JsonObject json = IIFileUtils.readJSONFile(IIReference.RES_PARTICLES.with(FILELOADER_ENTRIES.get(name).path.withExtension(ResLoc.EXT_FX_AMT).getResourcePath()));
				if(json.has("parent"))
				{
					String parent = json.get("parent").getAsString();
					graph.addEdge(parent, name);
				}
			} catch(Exception e)
			{
				IILogger.error("Couldn't load particle file "+FILELOADER_ENTRIES.get(name).path+", "+e.getMessage());
			}

		List<String> sortedNames = TopologicalSort.topologicalSort(graph);
		for(String name : sortedNames)
		{
			ParticleFileEntry entry = FILELOADER_ENTRIES.get(name);
			try
			{
				//Parse particle's JSON
				JsonObject json = IIFileUtils.readJSONFile(IIReference.RES_PARTICLES.with(entry.path.withExtension(ResLoc.EXT_FX_AMT).getResourcePath()));
				EasyNBT nbt = EasyNBT.wrapNBT(json);
				ParticleFactory<?> factory = TYPE_REGISTRY.get(nbt.getString("type")).get();
				nbt.checkSetString("parent", s -> factory.withParent(FILELOADER_ENTRIES.get(s).factory));
				factory.parseNBT(nbt);

				//Register the particle
				FACTORIES_REGISTRY.put(name, entry.factory = factory);

				//Register for model reload, if applicable
				if(factory instanceof IReloadableModelContainer)
					((IReloadableModelContainer<?>)factory).subscribeToList("particle/"+name);

			} catch(Exception e)
			{
				IILogger.error("Couldn't load particle file "+entry.path+", "+e.getMessage());
			}
		}
	}

	//--- Spawning Particles ---//

	/**
	 * Spawns a registered particle effect at the given position. Used by server messages.
	 *
	 * @param name The name of the particle
	 * @param nbt  The NBT data of the particle
	 */
	public static void spawnParticle(String name, EasyNBT nbt)
	{
		//check if contained in registry
		ParticleFactory<?> factory = FACTORIES_REGISTRY.get(name);
		if(factory==null)
			return;

		//saved by default in IIParticle
		factory.spawn(
				nbt.getVec3d(ParticleProperties.POSITION.getName()),
				nbt.getVec3d(ParticleProperties.MOTION.getName()),
				nbt.getVector2f(ParticleProperties.ROTATION.getName())
		).deserializeNBT(nbt.unwrap());
	}

	/**
	 * Spawns a registered particle effect at the given position, moving towards and facing the same direction.
	 * Overload of {@link #spawnParticle(String, Vec3d, Vec3d, Vector2f)}.
	 *
	 * @param name      The name of the particle
	 * @param pos       The position to spawn the particle at
	 * @param direction The direction the particle should move in
	 * @return The spawned particle
	 */
	@Nullable
	public static AbstractParticle spawnParticle(String name, Vec3d pos, Vector2f direction)
	{
		Vec3d motion = IIMath.offsetPosDirection(1, direction.x, direction.y).normalize();
		return spawnParticle(name, pos, motion, direction);
	}

	/**
	 * Spawns a registered particle effect at the given position, with different motion and rotation.
	 *
	 * @param name      The name of the particle
	 * @param pos       The position to spawn the particle at
	 * @param direction The direction the particle should move in
	 * @param motion    The motion of the particle
	 * @return The spawned particle
	 */
	@Nullable
	public static AbstractParticle spawnParticle(String name, Vec3d pos, Vec3d motion, Vector2f direction)
	{
		ParticleFactory<?> factory = FACTORIES_REGISTRY.get(name);
		if(factory!=null)
			return factory.spawn(pos, motion, direction);
		return null;
	}

	/**
	 * Spawns a registered particle effect at the given position, with different motion and facing directions.
	 *
	 * @param name           The name of the particle
	 * @param pos            The position to spawn the particle at
	 * @param motion         The motion of the particle
	 * @param directionYaw   The yaw direction the particle should face
	 * @param directionPitch The pitch direction the particle should face
	 * @return The spawned particle
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends AbstractParticle> T spawnParticle(Class<T> klass, String name, Vec3d pos, Vec3d motion, float directionYaw, float directionPitch)
	{
		ParticleFactory<?> factory = FACTORIES_REGISTRY.get(name);
		if(factory!=null)
			return ((T)factory.spawn(pos, motion, directionYaw, directionPitch));
		return null;
	}

	//--- External Info Methods ---//

	public static List<String> getRegisteredNames()
	{
		return new ArrayList<>(FACTORIES_REGISTRY.keySet());
	}

	//--- Old Methods ---//

	public static void spawnExplosionBoomFX(World world, Vec3d pos, Vec3d dir, IIExplosion explosion)
	{
		/*double powerFraction = explosion.getPower()*0.0625;
		int maxFractions = (int)Math.max(1, 4-Math.ceil(explosion.getSize()/4d));

		//normalize direction
		Vec3d explosionParticleDir = IIParticleUtils.normalizeExplosionDirection(dir);

		//spawn explosion center + smoke
		AbstractParticle explosionParticle = ParticleRegistry.spawnParticle(IIParticles.PARTICLE_EXPLOSION_TNT, pos,
				explosionParticleDir, Vec3d.ZERO);
		explosionParticle.setProperty(EasyNBT.newNBT().withDouble(SIZE, explosion.getSize()/5f));

		//defaultize direction
		dir = new Vec3d(0, 1, 0);

		//Custom sounds
		world.playSound(Minecraft.getMinecraft().player, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F+(world.rand.nextFloat()-world.rand.nextFloat())*0.2F)*0.7F);

		//Create debris for destroyed blocks at the end of explosion ray scan
		for(BlockPos destroyed : explosion.generateAffectedBlockPositions(true))
		{
			//Get the parameters of the destroyed block
			IBlockState state = world.getBlockState(destroyed);
			IPenetrationHandler penetrationHandler = PenetrationRegistry.getPenetrationHandler(state);
			String debrisName = penetrationHandler.getDebrisParticle();

			//Handler has no debris particle or it's not registered
			if(debrisName.isEmpty()||ParticleRegistry.getParticleBuilder(debrisName)==null)
				continue;

			//Get position and direction relative to the center of the explosion
			ResLoc resLoc = ResLoc.of(ClientUtils.getSideTexture(state, EnumFacing.DOWN));
			Vec3d desPos = new Vec3d(destroyed);
			Vec3d desDir = desPos.subtract(pos).normalize();

			//Random additional offset
			Vec3d offset = IIParticleUtils.getRandXZ().scale(0.05);

			//Create debris particle
			AbstractParticle particle = ParticleRegistry.spawnParticle(debrisName,
					desPos.add(offset).addVector(0, 0.5, 0),
					desDir,
					desDir.scale(0.0625)
							.add(dir.scale(powerFraction+IIParticleUtils.randFloat.get()*powerFraction))
			);
			assert particle instanceof ParticleAbstractModel;
			((ParticleAbstractModel)particle).retexture(0, resLoc);

			//Create smoke trace particle
			ParticleRegistry.spawnParticle(IIParticles.PARTICLE_SMOKE_TRACE,
							desPos.add(offset).addVector(0, 0.5, 0), desDir, Vec3d.ZERO)
					.withProperty(ParticleProperties.COLOR, IIColor.fromPackedRGB(0x373130))
					.withProperty(ParticleProperties.SCALE, 7f);

			//Create block chunk particles
			Vec3d finalDir = dir;
			IIParticleUtils.position(PositionGenerator.RAND_XZ, desPos.addVector(0, 0.5, 0), 1+Utils.RAND.nextInt(maxFractions), 0.05,
					(p, d) -> {
						AbstractParticle chunk = ParticleRegistry.spawnParticle(
								IIParticles.PARTICLE_BLOCK_CHUNK, p, desDir,
								desDir.scale(0.25*IIParticleUtils.randFloat.get())
										.add(finalDir.scale(powerFraction+IIParticleUtils.randFloat.get()*powerFraction))
						);
						assert chunk instanceof ParticleAbstractModel;
						((ParticleAbstractModel)chunk).retexture(0, resLoc);
					}
			);
		}*/

	}

	public static void spawnTracerFX(Vec3d pos, Vec3d motion, float size, int color)
	{
		/*ParticleTracer particle = new ParticleTracer(getWorld(), pos, motion, size, color);
		ParticleSystem.addEffect(particle);*/
	}

	//TODO: 04.05.2024 replace with AMT models
	public static void spawnGunfireFX(Vec3d pos, Vec3d motion, float size)
	{
		/*ParticleGunfire particle = new ParticleGunfire(getWorld(), pos, motion, size);
		ParticleSystem.addEffect(particle);*/
	}

	//TODO: 04.05.2024 replace with AMT models
	public static void spawnTMTModelFX(Vec3d pos, Vec3d motion, float size, ModelRendererTurbo model, ResourceLocation texture)
	{
		/*Particle particle = new ParticleTMTModel(getWorld(), pos, motion, size, model, texture);
		Minecraft.getMinecraft().effectRenderer.addEffect(particle);*/
	}

	public static void spawnFlameFX(Vec3d pos, Vec3d motion, float size, int lifeTime)
	{
		/*ParticleFlame particle = new ParticleFlame(getWorld(), pos, motion, size, lifeTime);
		ParticleSystem.addEffect(particle);*/
	}

	public static void spawnFlameExplosion(Vec3d pos, float size, Random rand)
	{

		for(int i = 0; i < 20*size; i += 1)
		{
			Vec3d v = new Vec3d(1, 0, 0).rotateYaw(i/20f*360f);

			ParticleCloud particle = (ParticleCloud)spawnVanillaParticle(EnumParticleTypes.CLOUD, pos, IIParticleUtils.withY(v.scale(0.25), 0.125));
			if(particle!=null)
			{
				particle.setRBGColorF(rand.nextFloat()*0.125f, rand.nextFloat()*0.125f, 0);
				particle.multipleParticleScaleBy(2.5f);
				particle.setMaxAge(10);
			}

		}


	}

	public static void spawnGasCloud(Vec3d pos, float size, Fluid fluid)
	{
		// Check if fluid is not null
		if(fluid==null) return;

		// Get the color of the fluid
		int color = fluid.getColor(); // Assuming the Fluid class has this method
		float red = ((color>>16)&255)/255.0F;
		float green = ((color>>8)&255)/255.0F;
		float blue = (color&255)/255.0F;

		// Spawn multiple particles for the gas cloud effect
		for(int i = 0; i < 40*size; i++)
		{
			// Randomly distribute particles in a larger spherical area
			double offsetX = (Math.random()-0.5)*size*3.5; // Increased spread
			double offsetY = (Math.random()-0.5)*size*2; // Increased vertical spread
			double offsetZ = (Math.random()-0.5)*size*3.5; // Increased spread

			Vec3d particlePos = pos.add(new Vec3d(offsetX, offsetY-1.0, offsetZ)); // Lower spawn point by 1 block

			ParticleCloud particle = (ParticleCloud)spawnVanillaParticle(EnumParticleTypes.CLOUD, particlePos, Vec3d.ZERO);
			if(particle!=null)
			{
				particle.setRBGColorF(red, green, blue); // Set the color of the particle
				particle.setMaxAge(160); // Adjust lifespan as needed
				particle.multipleParticleScaleBy(5f); // Adjust scale if necessary
			}
		}
	}

	//--- Utils ---//

	private static Particle spawnVanillaParticle(EnumParticleTypes particle, Vec3d pos, Vec3d motion)
	{
		return ClientUtils.mc().effectRenderer.spawnEffectParticle(particle.getParticleID(),
				pos.x, pos.y, pos.z,
				motion.x, motion.y, motion.z);
	}

	private static class ParticleFileEntry
	{
		ResLoc path;
		ParticleFactory<?> factory;

		ParticleFileEntry(ResLoc path)
		{
			this.path = path;
		}
	}
}