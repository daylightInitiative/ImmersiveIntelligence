package pl.pabilo8.immersiveintelligence.client.fx.utils;

import pl.pabilo8.immersiveintelligence.client.fx.particles.AbstractParticle;

/**
 * An executable program that can be attached to a particle to modify its behavior.
 *
 * @author Pabilo8 (pabilo@iiteam.net)
 * @ii-approved 0.3.1
 * @since 22.12.2024
 **/
public abstract class ParticleProgram
{
	private final String programName;

	public ParticleProgram(String programName)
	{
		this.programName = programName;
	}

	public void onParticleCreation(AbstractParticle particle)
	{

	}

	public void onParticleMovement(AbstractParticle particle)
	{

	}

	public void onParticleRender(AbstractParticle particle, float partialTicks)
	{

	}

	public void onParticleDeath(AbstractParticle particle)
	{

	}

	public String getProgramName()
	{
		return programName;
	}
}
