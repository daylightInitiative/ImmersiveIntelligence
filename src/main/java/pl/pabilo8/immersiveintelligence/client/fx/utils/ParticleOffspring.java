package pl.pabilo8.immersiveintelligence.client.fx.utils;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.util.math.Vec3d;
import pl.pabilo8.immersiveintelligence.client.fx.factories.ParticleFactory;
import pl.pabilo8.immersiveintelligence.client.fx.particles.AbstractParticle;
import pl.pabilo8.immersiveintelligence.client.fx.utils.IIParticleUtils.PositionGenerator;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

/**
 * @author Pabilo8 (pabilo@iiteam.net)
 * @ii-approved 0.3.1
 * @since 22.12.2024
 **/
public class ParticleOffspring<T extends AbstractParticle>
{
	ParticleFactory<T> particleFactory;
	PositionGenerator positionGenerator;
	float size;
	int minAmount;
	int maxAmount;

	public ParticleOffspring(ParticleFactory<T> particleFactory, PositionGenerator positionGenerator,
							 float size, int minAmount, int maxAmount)
	{
		this.particleFactory = particleFactory;
		this.positionGenerator = positionGenerator;
		this.size = size;
		this.minAmount = minAmount;
	}

	public ParticleOffspring(ParticleFactory<T> particleFactory, PositionGenerator positionGenerator,
							 float size, int amount)
	{
		this(particleFactory, positionGenerator, size, amount, amount);
	}

	public <O extends AbstractParticle> void spawn(O originParticle)
	{
		Vector3f originVector = (Vector3f)originParticle.getProperty(ParticleProperties.POSITION);
		Vec3d originPos = new Vec3d(originVector.x, originVector.y, originVector.z);

		int amount = minAmount+Utils.RAND.nextInt(maxAmount-minAmount);
		for(int i = 0; i < amount; i++)
		{
			Vec3d pos = positionGenerator.generatePosition(originPos, i, size, amount);
			Vec3d motion = positionGenerator.generateMotion(originPos, i, size, amount);
			Vector2f rotation = positionGenerator.generateRotation(originPos, i, size, amount);

			T particle = particleFactory.spawn(pos, motion, rotation.x, rotation.y);
		}
	}
}

