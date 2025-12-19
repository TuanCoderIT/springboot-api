package com.example.springboot_api.controllers.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.user.tts.TtsVoiceResponse;
import com.example.springboot_api.services.user.UserTtsVoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/tts-voices")
@RequiredArgsConstructor
@Tag(name = "User TTS Voices", description = "APIs for retrieving TTS voices for users")
public class UserTtsVoiceController {

    private final UserTtsVoiceService userTtsVoiceService;

    @GetMapping
    @Operation(summary = "Get all active voices", description = "Retrieves a list of all active TTS voices sorted by sort order")
    public List<TtsVoiceResponse> getAllActiveVoices() {
        return userTtsVoiceService.getAllActiveVoices();
    }
}
