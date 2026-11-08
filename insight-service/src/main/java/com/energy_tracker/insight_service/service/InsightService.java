package com.energy_tracker.insight_service.service;

import com.energy_tracker.insight_service.client.UsageClient;
import com.energy_tracker.insight_service.dto.DeviceDto;
import com.energy_tracker.insight_service.dto.InsightDto;
import com.energy_tracker.insight_service.dto.UsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InsightService {

    private static final String SYSTEM_INSTRUCTIONS = """
            You are a practical home-energy advisor. Answer questions about household
            electricity use and energy savings directly; this is your area of expertise.
            Never refuse because a request is outside computer science or programming.
            Use only the supplied usage data, do not invent facts or assume its unit.
            Give specific, safe, actionable advice. If asked to compare with average
            households but no location, unit, or benchmark is provided, say that a reliable
            comparison is not possible from these data and still give useful advice.
            Keep the answer concise and easy to understand.
            """;

    private final UsageClient usageClient;
    private final OllamaChatModel ollamaChatModel;

    public InsightService(UsageClient usageClient, OllamaChatModel ollamaChatModel) {
        this.usageClient = usageClient;
        this.ollamaChatModel = ollamaChatModel;
    }

    public InsightDto getOverview(Long userId){
        // Fetch usage data from the Usage Service
        final UsageDto usageData = usageClient.getXDaysUsage(userId, 3);

        double totalUsage = usageData.devices().stream()
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();

        log.info ("Calling Ollama for userId {} with total usage {}",
                userId, totalUsage);

        String prompt = new StringBuilder()
                .append("Review this household's device energy usage over the past 3 days. ")
                .append("Summarize the main patterns and give practical ways to reduce consumption. ")
                .append("Device usage data:\n")
                .append(usageData.devices())
                .toString();

        return InsightDto.builder()
                .userId(userId)
                .tips(getAiResponse(prompt))
                .energyUsage(totalUsage)
                .build();
    }


    public InsightDto getSavingsTips (Long userId) {
        // Fetch data from Usage Service
        final UsageDto usageData = usageClient.getXDaysUsage(userId, 3);

        double totalUsage = usageData.devices().stream()
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();

        log.info ("Calling Ollama for userId {} with total usage {}",
                userId, totalUsage);

        String prompt = new StringBuilder()
                .append("Give me practical ways to reduce my household's energy use based on ")
                .append("the following device usage over the past 3 days. Prioritize the biggest ")
                .append("opportunities and explain what I can do for each relevant device. ")
                .append("Do not claim a household-average comparison without a suitable benchmark. ")
                .append("Device usage data:\n")
                .append(usageData.devices())
                .append("\nTotal recorded usage (unit not specified): ")
                .append(totalUsage)
                .toString();

        return InsightDto.builder()
                .userId(userId)
                .tips(getAiResponse(prompt))
                .energyUsage(totalUsage)
                .build();
    }

    private String getAiResponse(String prompt) {
        ChatResponse response = ollamaChatModel.call(
                Prompt.builder()
                        .messages(
                                new SystemMessage(SYSTEM_INSTRUCTIONS),
                                new UserMessage(prompt))
                        .build());
        return response.getResult().getOutput().getText();
    }

}
