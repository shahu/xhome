package com.xhome.tcpserver.channelgroup;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import com.xhome.common.bean.DeviceMetadata;

public class XHomeChannelGroup {

    private static final AtomicInteger nextId = new AtomicInteger();

    private final String name;
    private final ConcurrentMap<String, Channel> clientChannels = new ConcurrentHashMap<String, Channel>();
    private final ChannelFutureListener remover = new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) throws Exception {
            DeviceMetadata  deviceData = (DeviceMetadata)future.getChannel().getAttachment();
            remove(deviceData.getSerialNum());
        }
    };

    public XHomeChannelGroup() {
        this("xhomegroup-0x" + Integer.toHexString(nextId.incrementAndGet()));
    }

    public XHomeChannelGroup(String name) {
        if(name == null) {
            throw new NullPointerException("name");
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return clientChannels.isEmpty();
    }

    public int size() {
        return clientChannels.size();
    }

    public Channel find(String key) {

        return clientChannels.get(key);
    }

    public boolean containsKey(String key) {

        return clientChannels.containsKey(key);
    }

    public boolean containsChannel(Channel channel) {

        return clientChannels.containsValue(channel);
    }

    public boolean add(String key, Channel channel) {
        ConcurrentMap<String, Channel> map = clientChannels;

        boolean added = map.putIfAbsent(key, channel) == null;

        if(added) {
            channel.getCloseFuture().addListener(remover);
        }

        return added;
    }

    public boolean remove(String key) {
        Channel c = clientChannels.remove(key);

        if(c == null) {
            return false;
        }

        c.getCloseFuture().removeListener(remover);
        return true;
    }


    public void clear() {
        clientChannels.clear();
    }

    public Iterator<Channel> iterator() {
        return clientChannels.values().iterator();
    }

    public Object[] toArray() {
        Collection<Channel> channels = new ArrayList<Channel>(size());
        channels.addAll(clientChannels.values());
        return channels.toArray();
    }


    public <T> T[] toArray(T[] a) {
        Collection<Channel> channels = new ArrayList<Channel>(size());
        channels.addAll(clientChannels.values());
        return channels.toArray(a);
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name: " + getName() + ", size: "
               + size() + ')';
    }

}
