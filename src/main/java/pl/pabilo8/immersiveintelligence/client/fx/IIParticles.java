package pl.pabilo8.immersiveintelligence.client.fx;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.pabilo8.immersiveintelligence.client.fx.factories.ParticleModelFactory;
import pl.pabilo8.immersiveintelligence.client.fx.factories.ParticleVanillaFactory;
import pl.pabilo8.immersiveintelligence.client.fx.particles.AbstractParticle;
import pl.pabilo8.immersiveintelligence.client.fx.particles.ParticleAMTModel;
import pl.pabilo8.immersiveintelligence.client.fx.particles.ParticleVanilla;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleProgram;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleProperties;
import pl.pabilo8.immersiveintelligence.client.fx.utils.ParticleRegistry;

/**
 * @author Pabilo8
 * @since 08.04.2024
 */
@SuppressWarnings("unchecked")
public class IIParticles
{
	//--- Particle IDs Reference ---//
	//Gunfire and related
	public static final String PARTICLE_GUNFIRE = "gunfire";

	//Debris Complimentary
	public static final String PARTICLE_BLOCK_CHUNK = "block/block_chunk";
	public static final String PARTICLE_BLOCK_IMPACT = "block/block_impact";
	public static final String PARTICLE_SMOKE_TRACE = "smoke/smoke_trace";
	//Explosions
	public static final String PARTICLE_SHOCKWAVE = "block/shockwave";
	public static final String PARTICLE_SHOCKWAVE_BIT = "explosion_shockwave_bit";
	public static final String PARTICLE_EXPLOSION_TNT = "explosion_tnt";
	public static final String PARTICLE_EXPLOSION_TNT_LIGHT = "explosion_tnt_light";

	@SideOnly(Side.CLIENT)
	public static void preInit()
	{
		//Register particle types
		ParticleRegistry.registerParticleType("ParticleModel", () -> new ParticleModelFactory<>(ParticleAMTModel::new));
		ParticleRegistry.registerParticleType("ParticleVanilla", () -> new ParticleVanillaFactory(ParticleVanilla::new));

		//Register programs
		ParticleRegistry.registerProgram(new ParticleProgram("dust_transition")
		{
			@Override
			public void onParticleRender(AbstractParticle particle, float partialTicks)
			{
				super.onParticleRender(particle, partialTicks);
				float progress = particle.getProgress(0);
				particle.setProperty(ParticleProperties.SCALE, 1+progress*0.5);
				particle.setProperty(ParticleProperties.ALPHA, 1-progress);
			}
		});
		ParticleRegistry.registerProgram(new ParticleProgram("smoke_transition")
		{
			@Override
			public void onParticleRender(AbstractParticle particle, float partialTicks)
			{
				float progress = particle.getProgress(0);
				particle.setProperty(ParticleProperties.SCALE, 1+progress);

				float alpha;
				if(progress < 0.2)
					alpha = (progress/0.2f)*0.75f;
				else if(progress < 0.7)
					alpha = 0.75f-0.5f*((progress-0.2f)/0.5f);
				else
					alpha = 0.25f*(1-(progress-0.7f)/0.3f);

				particle.setProperty(ParticleProperties.ALPHA, alpha);
			}
		});
		ParticleRegistry.registerProgram(new ParticleProgram("shockwave_transition")
		{
			@Override
			public void onParticleRender(AbstractParticle particle, float partialTicks)
			{
				float progress = particle.getProgress(0);
				particle.setProperty(ParticleProperties.SCALE, 1+progress*2);
				particle.setProperty(ParticleProperties.ALPHA, 1-progress);
			}
		});
		ParticleRegistry.registerProgram(new ParticleProgram("lifetime_retexture")
		{
			@Override
			public void onParticleRender(AbstractParticle particle, float partialTicks)
			{
				int textures = (int)particle.getProperty(ParticleProperties.TEXTURES_COUNT);
				particle.setProperty(ParticleProperties.TEXTURE_SHIFT, (int)(particle.getProgress(partialTicks)*textures));
			}
		});
		ParticleRegistry.registerProgram(new ParticleProgram("debris_rotation")
		{
			@Override
			public void onParticleMovement(AbstractParticle particle)
			{
				super.onParticleMovement(particle);
				if(!((Boolean)particle.getProperty(ParticleProperties.ON_GROUND)))
				{
					float yaw = MathHelper.wrapDegrees((float)particle.getProperty(ParticleProperties.ROTATION_YAW)+2f);
					particle.setProperty(ParticleProperties.ROTATION_YAW, yaw);
					float pitch = MathHelper.wrapDegrees((float)particle.getProperty(ParticleProperties.ROTATION_PITCH)+2f);
					particle.setProperty(ParticleProperties.ROTATION_PITCH, pitch);
				}
			}
		});
	}

	@SideOnly(Side.CLIENT)
	public static void init()
	{
		ParticleRegistry.registerParticle(PARTICLE_BLOCK_CHUNK);
		ParticleRegistry.registerParticle(PARTICLE_BLOCK_IMPACT);
		ParticleRegistry.registerParticle(PARTICLE_SMOKE_TRACE);
		ParticleRegistry.registerParticle(PARTICLE_SHOCKWAVE);
		ParticleRegistry.registerParticle(PARTICLE_GUNFIRE);
	}
}
