package com.pujiyam.chatter.API;

import com.pujiyam.chatter.model.UserListing;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class UserListingAPI {

    @GetMapping("/my-listings")
    public ResponseEntity<List<UserListing>> getListings() {
        var listing = List.of(
                new UserListing("Grandview", "123 Anywhere St", "OH", "USA", BigDecimal.valueOf(50), "Available"),
                new UserListing("Polaris", "456 Somewhere Rd", "OH", "USA", BigDecimal.valueOf(35), "Taken")
        );

        return ResponseEntity.ok(listing);
    }
}
