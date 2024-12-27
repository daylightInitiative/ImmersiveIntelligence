package pl.pabilo8.immersiveintelligence.client.fx.factories;

import blusunrize.immersiveengineering.client.ClientUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.pabilo8.immersiveintelligence.client.fx.particles.AbstractParticle;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleProgram;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleProperties;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleRegistry;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleSystem;
import pl.pabilo8.immersiveintelligence.common.util.easynbt.EasyNBT;

import javax.annotation.Nonnull;
import javax.vecmath.Vector2f;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Used to build and spawn particle effects.
 *
 * @author Pabilo8
 * @ii-approved 0.3.1
 * @since 05.04.2024
 */
@SideOnly(Side.CLIENT)
public class ParticleFactory<T extends AbstractParticle>
{
	private final Set<Consumer<T>> chained = new HashSet<>();
	private final Multimap<Integer, Consumer<T>> scheduled = HashMultimap.create();
	private final BiFunction<World, Vec3d, T> particleType;
	private final Map<ParticleProperties, Object> properties = new HashMap<>();
	private Set<ParticleProgram> programs = Collections.emptySet();

	public ParticleFactory(BiFunction<World, Vec3d, T> particleConstructor)
	{
		//Set particle type
		this.particleType = particleConstructor;
		//Set default values
		for(ParticleProperties property : ParticleProperties.values())
			if(!property.isNBT())
				properties.put(property, property.getDefault());
	}

	/**
	 * Sets a property of the particle effect.
	 *
	 * @param property The property to set.
	 * @param value    The value to set the property to.
	 * @return The particle factory instance.
	 */
	public final ParticleFactory<T> withProperty(ParticleProperties property, Object value)
	{
		properties.put(property, value);
		return this;
	}

	/**
	 * Removes a property from the particle effect.
	 *
	 * @param property The property to remove.
	 * @return The particle factory instance.
	 */
	public final ParticleFactory<T> withoutProperty(ParticleProperties property)
	{
		properties.remove(property);
		return this;
	}

	/**
	 * Bases the particle effect on another particle effect.
	 *
	 * @param parentFactory Another particle effect factory.
	 * @return The particle factory instance.
	 */
	public ParticleFactory<T> withParent(ParticleFactory<?> parentFactory)
	{
		this.properties.putAll(parentFactory.properties);
//		this.chained.addAll(parentFactory.chained);
//		this.scheduled.putAll(parentFactory.scheduled);
		this.programs = new HashSet<>(parentFactory.programs);
		return this;
	}

	/**
	 * Adds a chained particle effect to the particle effect.
	 *
	 * @param chained The chained particle effect.
	 * @return The particle factory instance.
	 */
	public ParticleFactory<T> withChainedParticle(Consumer<T> chained)
	{
		this.chained.add(chained);
		return this;
	}

	/**
	 * Adds a scheduled particle effect to the particle effect.
	 *
	 * @param delay     the delay in ticks
	 * @param scheduled the scheduled particle effect
	 * @return The particle factory instance.
	 */
	public ParticleFactory<T> withScheduledParticle(int delay, Consumer<T> scheduled)
	{
		this.scheduled.put(delay, scheduled);
		return this;
	}

	/**
	 * Loads particle effect properties from NBT (Json).
	 *
	 * @param nbt The NBT to load the properties from.
	 */
	public void parseNBT(EasyNBT nbt)
	{
		//Parse properties
		for(ParticleProperties property : ParticleProperties.values())
			if(nbt.hasKey(property.getName()))
			{
				Object value = property.getPropertyFromNBT(nbt);
				if(value!=null)
					withProperty(property, value);
			}

		//Parse programs
		Set<ParticleProgram> parsedPrograms = nbt.streamList(NBTTagString.class, "programs", EasyNBT.TAG_STRING)
				.map(NBTTagString::getString)
				.map(ParticleRegistry::getProgram)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		//Join programs, programs can be provided by both a parent particle and the NBT
		this.programs = new ImmutableSet.Builder<ParticleProgram>()
				.addAll(programs)
				.addAll(parsedPrograms)
				.build();
	}

	/**
	 * Creates a particle effect without spawning it.
	 *
	 * @param position The position of the particle effect.
	 * @param motion   The motion of the particle effect.
	 * @param rotation The rotation of the particle effect.
	 * @return The particle effect.
	 */
	public T create(Vec3d position, Vec3d motion, Vector2f rotation)
	{
		T particle = particleType.apply(ClientUtils.mc().world, position);
		properties.forEach(particle::setProperty);
		particle.setPMR(position, motion, rotation);
		particle.setChainedParticles(chained);
		particle.setScheduledParticles(scheduled);
		particle.setPrograms(programs);

		return particle;
	}

	@Nonnull
	public final T create(Vec3d position, Vec3d motion, float rotationYaw, float rotationPitch)
	{
		return create(position, motion, new Vector2f(rotationYaw, rotationPitch));
	}

	/**
	 * Spawns a particle effect.
	 *
	 * @param position The position of the particle effect.
	 * @param motion   The motion of the particle effect.
	 * @return The particle effect.
	 */
	@Nonnull
	public final T spawn(Vec3d position, Vec3d motion)
	{
		return spawn(position, motion, 0, 0);
	}

	@Nonnull
	public final T spawn(Vec3d position, Vec3d motion, float rotationYaw, float rotationPitch)
	{
		return spawn(position, motion, new Vector2f(rotationYaw, rotationPitch));
	}

	public final T spawn(Vec3d position, Vec3d motion, Vector2f rotation)
	{
		T particle = create(position, motion, rotation);
		ParticleSystem.INSTANCE.addEffect(particle);
		return particle;
	}

	/**
	 * Schedules a particle effect to spawn after a delay.
	 *
	 * @param position The position of the particle effect.
	 * @param motion   The motion of the particle effect.
	 * @param delay    The delay in ticks.
	 * @return The particle effect.
	 */
	@Nonnull
	public final T scheduleSpawn(Vec3d position, Vec3d motion, int delay)
	{
		return scheduleSpawn(position, motion, 0, 0, delay);
	}

	@Nonnull
	public final T scheduleSpawn(Vec3d position, Vec3d motion, float rotationYaw, float rotationPitch, int delay)
	{
		T particle = create(position, motion, rotationYaw, rotationPitch);
		ParticleSystem.INSTANCE.scheduleEffect(particle, delay);
		return particle;
	}
}