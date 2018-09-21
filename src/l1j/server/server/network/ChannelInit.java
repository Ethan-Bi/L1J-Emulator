/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */

/*
 * (Tricid)  This is the first class in the pipeline that handles new connections. Right now I'm only
 * using it to send the first packet that the client expects.
 */
package l1j.server.server.network;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import l1j.server.server.encryptions.ClientIdExistsException;
import l1j.server.server.encryptions.L1JEncryption;

public class ChannelInit extends ChannelInitializer<Channel> {

	Logger _log = LoggerFactory.getLogger(ChannelInit.class);

	private static final byte[] FIRST_PACKET = { // 3.0 English KeyPacket
			(byte) 0x41, (byte) 0x5A, (byte) 0x9B, (byte) 0x01, (byte) 0xB6, (byte) 0x81, (byte) 0x01, (byte) 0x09,
			(byte) 0xBD, (byte) 0xCC, (byte) 0xC0 };
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
	    cause.printStackTrace();
	    ctx.close();
		_log.info("Exception happened");
		Client client = NetworkServer.getInstance().getClients().get(ctx.channel().id());
		client.handleDisconnect();
		NetworkServer.getInstance().getClients().remove(ctx.channel().id());
	}
	@Override
	protected void initChannel(Channel channel) throws Exception {
		final ByteBuf first = channel.alloc().buffer(FIRST_PACKET.length + 7);
		Client client = null;
		try {
			client = new Client(channel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NetworkServer.getInstance().getClients().put(channel.id(), client);

		long seed = 0x7C98BDFA; // 3.0 English Packet Seed
		byte Bogus = (byte) (FIRST_PACKET.length + 7);

		first.writeByte(Bogus & 0xFF);
		first.writeByte(Bogus >> 8 & 0xFF);
		first.writeByte(0x7D);
		first.writeByte((byte) (seed & 0xFF));
		first.writeByte((byte) (seed >> 8 & 0xFF));
		first.writeByte((byte) (seed >> 16 & 0xFF));
		first.writeByte((byte) (seed >> 24 & 0xFF));
		first.writeBytes(FIRST_PACKET);
		channel.writeAndFlush(first);

		try {
			client.set_clkey(L1JEncryption.initKeys(channel.id(), seed));
		} catch (ClientIdExistsException e) {
			e.printStackTrace();
		}
		channel.pipeline().addLast(new PacketDecoder(),new PacketDecrypter());

	}

	@Override
	public void channelActive(final ChannelHandlerContext channel) {

	}
}
