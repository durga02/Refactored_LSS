
package com.lalitha.sweets.controller;

import org.springframework.web.bind.annotation.*;
import com.lalitha.sweets.model.FestivalBanner;
import com.lalitha.sweets.repository.FestivalBannerRepository;

@RestController
@RequestMapping("/api/admin/festival-banner")
public class AdminFestivalBannerController {

    private final FestivalBannerRepository repository;

    public AdminFestivalBannerController(
            FestivalBannerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public FestivalBanner getSettings() {
        return repository.findById(1L)
                .orElseGet(() -> repository.save(new FestivalBanner()));
    }

    @PutMapping
    public FestivalBanner saveSettings(
            @RequestBody FestivalBanner request) {

        FestivalBanner banner = repository.findById(1L)
                .orElseGet(FestivalBanner::new);

        banner.setMessage(
                request.getMessage() == null
                        ? ""
                        : request.getMessage().trim());

        banner.setEnabled(request.isEnabled());

        String color = request.getBackgroundColor();
        if (color != null && color.matches("#[0-9a-fA-F]{6}")) {
            banner.setBackgroundColor(color);
        }

        banner.setScrolling(request.isScrolling());

        return repository.save(banner);
    }
}
