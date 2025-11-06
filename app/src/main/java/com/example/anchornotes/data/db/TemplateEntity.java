package com.example.anchornotes.data.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "templates")
public class TemplateEntity implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;                    // "Meeting Notes", "Shopping List"
    public String pageColor;               // "#E3F2FD", "#FFEBEE" (hex colors)
    public String prefilledHtml;           // HTML content for note body
    public String associatedTagIds;        // JSON: "[1,2,3]" or null
    public String associatedGeofenceId;    // "office", "home" or null
    public boolean isExample;              // true for seeded example template
    public long createdAt;
    public long updatedAt;

    public TemplateEntity() {} // Room constructor

    @Ignore
    public TemplateEntity(String name, String pageColor, String prefilledHtml,
                         String associatedTagIds, String associatedGeofenceId,
                         boolean isExample) {
        this.name = name;
        this.pageColor = pageColor;
        this.prefilledHtml = prefilledHtml;
        this.associatedTagIds = associatedTagIds;
        this.associatedGeofenceId = associatedGeofenceId;
        this.isExample = isExample;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }
}