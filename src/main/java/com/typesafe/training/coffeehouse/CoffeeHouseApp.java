/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */
package com.typesafe.training.coffeehouse;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Future;
import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoffeeHouseApp implements Terminal{

    public static final Pattern optPattern = Pattern.compile("(\\S+)=(\\S+)");

    private final ActorSystem system;

    private final LoggingAdapter log;

    private final int caffeineLimit;

    private Timeout statusTimeout;

    @SuppressWarnings("unused")
    private final ActorRef coffeeHouse;

    public CoffeeHouseApp(final ActorSystem system, Timeout statusTimeout){
        this.system = system;
        caffeineLimit = system.settings().config().getInt("coffee-house.caffeine-limit");
        log = Logging.getLogger(system, getClass().getName());
        coffeeHouse = createCoffeeHouse();
        this.statusTimeout = statusTimeout;

        // Prepare an anonymous actor
//        Props props = Props.create(AbstractLoggingActor.class, () -> new AbstractLoggingActor() {
//            {
//                coffeeHouse.tell("Brew coffee", self());
//                receive(ReceiveBuilder.matchAny(o -> {
//                    log().info(o.toString());
//                    system.stop(self());
//                }).build());
//            }
//        });

        // Actually create the anonymous actor
//        system.actorOf(props);

    }

    public static void main(final String[] args) throws IOException{
        final Map<String, String> opts = argsToOpts(Arrays.asList(args));
        applySystemProperties(opts);
        final String name = opts.getOrDefault("name", "coffee-house");

        final ActorSystem system = ActorSystem.create(String.format("%s-system", name));

        Timeout statusTimeout = new Timeout(
                system.settings().config().getDuration("coffee-house.status-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);

        final CoffeeHouseApp coffeeHouseApp = new CoffeeHouseApp(system, statusTimeout);
        coffeeHouseApp.run();
    }

    public static Map<String, String> argsToOpts(final List<String> args){
        final Map<String, String> opts = new HashMap<>();
        for (final String arg : args) {
            final Matcher matcher = optPattern.matcher(arg);
            if (matcher.matches()) opts.put(matcher.group(1), matcher.group(2));
        }
        return opts;
    }

    public static void applySystemProperties(final Map<String, String> opts){
        opts.forEach((key, value) -> {
            if (key.startsWith("-D")) System.setProperty(key.substring(2), value);
        });
    }

    private void run() throws IOException{
        log.warning(
            String.format("{} running%nEnter commands into the terminal, e.g. 'q' or 'quit'"),
            getClass().getSimpleName()
        );
        commandLoop();
        system.awaitTermination();
    }

    protected ActorRef createCoffeeHouse(){
//        return system.deadLetters();

        return system.actorOf(CoffeeHouse.props(caffeineLimit), "coffee-house");
    }

    private void commandLoop() throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            TerminalCommand tc = Terminal.create(in.readLine());
            if (tc instanceof TerminalCommand.Guest) {
                TerminalCommand.Guest tcg = (TerminalCommand.Guest) tc;
                createGuest(tcg.count, tcg.coffee, tcg.maxCoffeeCount);
            } else if (tc == TerminalCommand.Status.Instance) {
                getStatus();
            } else if (tc == TerminalCommand.Quit.Instance) {
                system.shutdown();
                break;
            } else {
                TerminalCommand.Unknown u = (TerminalCommand.Unknown) tc;
                log.warning("Unknown terminal command {}!", u.command);
            }
        }
    }

    protected void createGuest(int count, Coffee coffee, int maxCoffeeCount){
        for (int i = 0; i < count; ++i) {
            coffeeHouse.tell(new CoffeeHouse.CreateGuest(coffee, maxCoffeeCount), ActorRef.noSender());
        }
    }

    protected void getStatus(){
        ClassTag<CoffeeHouse.Status> tag = ClassTag$.MODULE$.apply(CoffeeHouse.Status.class);
        Future<Object> future = Patterns.ask(coffeeHouse, CoffeeHouse.GetStatus.Instance, statusTimeout);
        future.mapTo(tag).onComplete(new OnComplete<CoffeeHouse.Status>(){
            @Override
            public void onComplete(Throwable failure, CoffeeHouse.Status success) throws Throwable {
                log.info("Guest count: {}", success.guestCount);
            }
        }, system.dispatcher());
    }
}
