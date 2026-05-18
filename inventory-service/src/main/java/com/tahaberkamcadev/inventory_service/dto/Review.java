package com.tahaberkamcadev.inventory_service.dto;

import java.util.Date;

public record Review(Date reviewDate, String reviewerName, int rating, String comment) {}
