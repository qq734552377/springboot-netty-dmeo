package com.netty.client.handler;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.netty.common.protobuf.Message;
import com.netty.client.NettyClient;
import com.netty.common.protobuf.Command.CommandType;
import com.netty.common.protobuf.Message.MessageBase;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class IdleClientHandler extends SimpleChannelInboundHandler<Message> {
	public Logger log = Logger.getLogger(this.getClass());	

	private NettyClient nettyClient;
	private int heartbeatCount = 0;
	private final static String CLIENTID = "123456789";

	/**
	 * @param nettyClient
	 */
	public IdleClientHandler(NettyClient nettyClient) {
		this.nettyClient = nettyClient;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			String type = "";
			if (event.state() == IdleState.READER_IDLE) {
				type = "read idle";
			} else if (event.state() == IdleState.WRITER_IDLE) {
				type = "write idle";
				sendPingMsg(ctx);
			} else if (event.state() == IdleState.ALL_IDLE) {
				type = "all idle";
			}
			log.debug(ctx.channel().remoteAddress() + "超时类型：" + type);
			String id = ctx.channel().id().toString();
			System.out.println("ctx超时的时候的id-->" + id);

		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	/**
	 * 发送ping消息
	 * @param context
	 */
	protected void sendPingMsg(ChannelHandlerContext context) {
		try {
			context.writeAndFlush(
					MessageBase.newBuilder()
							.setClientId(CLIENTID)
							.setCmd(CommandType.PING)
							.setData("This is a ping msg")
							.build()
			);
		}catch (Exception e){
			System.out.println(e.toString());
		}

		heartbeatCount++;
		log.info("Client sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
	}

	/**
	 * 处理断开重连
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		final EventLoop eventLoop = ctx.channel().eventLoop();
		eventLoop.schedule(new Runnable() {
			@Override
			public void run() {
				log.info("客户端尝试重新连接");
				nettyClient.doConnect(new Bootstrap(), eventLoop);
			}
		} ,10L, TimeUnit.SECONDS);
		log.info("客户端的连接被关闭-->" + ctx.channel().id().toString());
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

	}
}
