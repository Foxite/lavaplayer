package com.sedmelluq.discord.lavaplayer.demo;

import net.dv8tion.jda.api.JDABuilder;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

public class Bootstrap {
  public static void main(String[] args) throws Exception {
    JDABuilder.create("OTU4MzI3MzkwOTU2ODE0NDU5.GcQXw0.1eMRnMppdV1QElqYub8-mIw5nx7agpsyTG-Wbg", GUILD_MESSAGES, GUILD_VOICE_STATES)
        .addEventListeners(new BotApplicationManager())
        .build();
  }
}
