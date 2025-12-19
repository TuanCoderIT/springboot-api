package com.example.springboot_api.services.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các thao tác liên quan đến Voice (danh sách giọng đọc TTS).
 */
@Service
@RequiredArgsConstructor
public class VoiceService {

  /**
   * Lấy danh sách các giọng đọc (voices) có sẵn từ Gemini TTS.
   * 
   * @return Danh sách VoiceInfo
   */
  public List<VoiceInfo> getVoices() {
    List<VoiceInfo> voices = new ArrayList<>();

    // Danh sách voices từ Gemini 2.5 Flash TTS
    // Tham khảo: https://ai.google.dev/gemini-api/docs/audio

    // Giọng nữ
    voices.add(new VoiceInfo("Aoede", "female", "en-US", "Warm, friendly female voice"));
    voices.add(new VoiceInfo("Kore", "female", "en-US", "Clear, professional female voice"));

    // Giọng nam
    voices.add(new VoiceInfo("Puck", "male", "en-US", "Energetic, dynamic male voice"));
    voices.add(new VoiceInfo("Charon", "male", "en-US", "Deep, authoritative male voice"));

    // Giọng đa ngôn ngữ (nếu có)
    voices.add(new VoiceInfo("Fenrir", "male", "multi", "Versatile male voice"));
    voices.add(new VoiceInfo("Orbit", "female", "multi", "Versatile female voice"));

    return voices;
  }

  /**
   * Record chứa thông tin về một giọng đọc.
   */
  public record VoiceInfo(
      String voiceId,
      String gender,
      String language,
      String description) {
  }
}
