package com.example.aiservice.web;

import com.example.aiservice.analysis.AnalysisService;
import com.example.aiservice.api.AnalyzeRequest;
import com.example.aiservice.api.AnalyzeResponse;
import com.example.aiservice.api.InfoResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AnalyzeController {

    private final AnalysisService analysisService;

    public AnalyzeController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return analysisService.analyze(request.subject(), request.body());
    }

    @GetMapping("/info")
    public InfoResponse info() {
        return new InfoResponse(analysisService.activeEngine(), analysisService.model());
    }
}
