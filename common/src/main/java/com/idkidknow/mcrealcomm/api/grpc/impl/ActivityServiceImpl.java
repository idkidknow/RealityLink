package com.idkidknow.mcrealcomm.api.grpc.impl;

import com.idkidknow.mcrealcomm.grpc.Activity;
import com.idkidknow.mcrealcomm.grpc.ActivityServiceGrpc;
import com.idkidknow.mcrealcomm.grpc.Empty;
import com.mojang.logging.LogUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityServiceImpl extends ActivityServiceGrpc.ActivityServiceImplBase {
    private static final Logger logger = LogUtils.getLogger();
    private final Set<StreamObserver<Activity>> OBSERVERS = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );

    @Override
    public StreamObserver<Empty> subscribeActivity(StreamObserver<Activity> responseObserver) {
        return new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                OBSERVERS.add(responseObserver);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("subscribeActivity error: ", t);
                OBSERVERS.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                OBSERVERS.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }

    public void sendActivity(Activity activity) {
        for (var observer : OBSERVERS) {
            observer.onNext(activity);
        }
    }

    public void shutdown() {
        for (var observer : OBSERVERS) {
            observer.onCompleted();
        }
        OBSERVERS.clear();
    }
}
