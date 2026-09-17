package com.fris.begems.framework;

import com.fris.begems.framework.dto.FrameworkDetail;
import com.fris.begems.framework.dto.QuestionSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frameworks")
public class FrameworkController {

    private final FrameworkService frameworkService;

    public FrameworkController(FrameworkService frameworkService) {
        this.frameworkService = frameworkService;
    }

    @GetMapping("/active")
    public FrameworkDetail getActive() {
        return frameworkService.getActiveFramework();
    }

    @GetMapping("/active/questions")
    public List<QuestionSummary> getQuestions(@RequestParam EvaluationType evaluationType) {
        return frameworkService.getQuestions(evaluationType);
    }
}
