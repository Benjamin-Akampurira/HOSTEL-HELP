package edu.hostelhelp.controller;

import edu.hostelhelp.dto.ApiDtos.ReportUpdate;
import edu.hostelhelp.dto.ApiDtos.ReportView;
import edu.hostelhelp.dto.ApiDtos.WardenOverview;
import edu.hostelhelp.dto.ApiDtos.WardenReportDetails;
import edu.hostelhelp.enums.ReportStatus;
import edu.hostelhelp.service.HostelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warden")
@PreAuthorize("hasRole('WARDEN')")
public class WardenController {
    private final HostelService service;

    public WardenController(HostelService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public List<ReportView> reports(@RequestParam(required = false) ReportStatus status) {
        return service.reports(status);
    }

    @GetMapping("/overview")
    public WardenOverview overview() {
        return service.wardenOverview();
    }

    @GetMapping("/reports/{id}")
    public WardenReportDetails reportDetails(@PathVariable long id) {
        return service.reportDetails(id);
    }

    @PatchMapping("/reports/{id}")
    public ReportView update(@PathVariable long id, @Valid @RequestBody ReportUpdate input) {
        return service.updateReport(id, input);
    }
}