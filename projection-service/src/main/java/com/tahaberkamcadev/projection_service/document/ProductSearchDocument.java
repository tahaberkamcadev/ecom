package com.tahaberkamcadev.projection_service.document;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(shards = 1, replicas = 0)
@Document(indexName = "product_catalog")
public class ProductSearchDocument {

    @Id
    private String productId;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String brand;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Boolean)
    private boolean inStock;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Double)
    private BigDecimal averageRating;

    @Field(type = FieldType.Integer)
    private int reviewCount;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
}
