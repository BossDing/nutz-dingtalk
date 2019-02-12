package com.rekoe.websocket;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.nutz.dao.Dao;
import org.nutz.integration.jedis.JedisAgent;
import org.nutz.integration.jedis.pubsub.PubSub;
import org.nutz.integration.jedis.pubsub.PubSubService;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.plugins.mvc.websocket.AbstractWsEndpoint;
import org.nutz.plugins.mvc.websocket.NutWsConfigurator;
import org.nutz.plugins.mvc.websocket.WsHandler;
import org.nutz.plugins.mvc.websocket.room.JedisRoomProvider;

import redis.clients.jedis.Jedis;

@ServerEndpoint(value = "/websocket", configurator = NutWsConfigurator.class)
@IocBean(create = "init")
public class OmWebsocket extends AbstractWsEndpoint implements PubSub {

	protected static final Log log = Logs.get();

	@Inject
	protected PubSubService pubSubService;

	@Inject
	private Dao dao;

	@Inject
	protected JedisAgent jedisAgent;

	public void init() {
		roomPrefix = "wsroom:";
		roomProvider = new JedisRoomProvider(jedisAgent);
		try (Jedis jedis = jedisAgent.getResource()) {
			for (String key : jedis.keys(roomPrefix + "*")) {
				switch (jedis.type(key)) {
				case "none":
					break;
				case "set":
					break;
				default:
					jedis.del(key);
				}
			}
		}
		pubSubService.reg(roomPrefix + "*", this);
	}

	@Override
	public void onMessage(String channel, String message) {
		if (log.isDebugEnabled())
			log.debugf("GET PubSub channel=%s msg=%s", channel, message);
		each(channel, (index, session, length) -> session.getAsyncRemote().sendText(message));
	}

	@Override
	public WsHandler createHandler(Session session, EndpointConfig config) {
		OmWsHandler handler = new OmWsHandler(roomPrefix, jedisAgent,dao);
		handler.setPubSubService(pubSubService);
		return handler;
	}
}
