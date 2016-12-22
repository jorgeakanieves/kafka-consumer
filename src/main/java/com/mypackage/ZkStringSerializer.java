package com.mypackage;

import kafka.utils.ZKStringSerializer;
import org.I0Itec.zkclient.serialize.ZkSerializer;

public class ZkStringSerializer implements ZkSerializer { 
 
    @Override
    public byte[] serialize(Object data){
        return ZKStringSerializer.serialize(data); 
    } 
 
    @Override
    public Object deserialize(byte[] bytes){
        return ZKStringSerializer.deserialize(bytes); 
    } 
 
}