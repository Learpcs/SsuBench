package com.rodin.SsuBench.Controller;

import com.rodin.SsuBench.Config.UserDetailsImpl;
import com.rodin.SsuBench.Controller.Request.Bid.CreateBidRequest;
import com.rodin.SsuBench.Controller.Response.Bid.BidResponse;
import com.rodin.SsuBench.Controller.Response.PageResponse;
import com.rodin.SsuBench.Service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/task/{taskId}")
    public ResponseEntity<BidResponse> createBid(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateBidRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        BidResponse response = bidService.createBid(request, userDetails.getId(), taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BidResponse> getBid(@PathVariable Long id) {
        BidResponse response = bidService.getBid(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<PageResponse<BidResponse>> getBidsByTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<BidResponse> response = bidService.getBidsByTask(taskId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<BidResponse>> getMyBids(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<BidResponse> response = bidService.getBidsByExecutor(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<BidResponse> acceptBid(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        BidResponse response = bidService.acceptBid(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<BidResponse> rejectBid(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        BidResponse response = bidService.rejectBid(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BidResponse> cancelBid(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        BidResponse response = bidService.cancelBid(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
