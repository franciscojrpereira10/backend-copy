package pt.ipleiria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Timestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "comments")
public class Comment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 2000, nullable = false)
    private String content;

    @Timestamp
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "visible_reason")
    private String visibleReason;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Timestamp
    @Column(name = "edited_at")
    private Date editedAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    public Comment() {
        this.createdAt = new Date();
        this.visible = true;
    }

    public Long getId() {
        return id;
    }

    public @NotBlank String getContent() {
        return content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getVisibleReason() {
        return visibleReason;
    }

    public User getAuthor() {
        return author;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(@NotBlank String content) {
        this.content = content;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setVisibleReason(String visibleReason) {
        this.visibleReason = visibleReason;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    // metodos auxiliares

    public void editContent(String newContent) {
        this.content = newContent;
        this.editedAt = new Date();
    }

    public void changeVisibility(boolean visible, String reason) {
        this.visible = visible;
        this.visibleReason = reason;
    }

    public boolean isEdited() {
        return this.editedAt != null;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }


}

