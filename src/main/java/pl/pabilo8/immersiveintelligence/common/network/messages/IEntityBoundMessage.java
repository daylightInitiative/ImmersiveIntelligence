package pl.pabilo8.immersiveintelligence.common.network.messages;

import net.minecraft.entity.Entity;
import pl.pabilo8.immersiveintelligence.common.network.IIPacketHandler;

public interface IEntityBoundMessage
{
	Entity getEntity();

	default int getPacketDistance()
	{
		return IIPacketHandler.DEFAULT_RANGE;
	}
}
