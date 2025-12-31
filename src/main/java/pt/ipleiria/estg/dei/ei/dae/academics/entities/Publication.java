package pt.ipleiria.estg.dei.ei.dae.academics.entities;

import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;
import jakarta.persistence.*;
import java.util.*;

@NamedQueries({
        // ===== EP07 - Publicações do próprio =====
        @NamedQuery(
                name = "Publication.findByUploader",
                query = "SELECT p FROM Publication p " +
                        "WHERE p.uploadedBy = :uploader " +
                        "ORDER BY p.createdAt DESC"
        )
})
@Entity
@Table(name = "publications")

public class Publication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String authors;

    @Column(nullable = false, length = 200)
    private String scientificArea;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "visibility_reason")
    private String visibilityReason;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(name="file_type", nullable=false, length=10)
    private FileType fileType;

    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt = new Date();

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "publication_tags",
            joinColumns = @JoinColumn(name = "publication_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags;

    @OneToMany(fetch = FetchType.LAZY)
    private List<Comment> comments;

    @OneToMany(mappedBy = "publication", fetch = FetchType.LAZY)
    private List<Rating> ratings;

    @OneToMany(mappedBy = "publication", fetch = FetchType.LAZY)
    private List<PublicationHistory> history;

    public Publication() {
        tags = new ArrayList<>();
        comments = new ArrayList<>();
        ratings = new ArrayList<>();
        history = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Publication(Long id, String title, String summary, String scientificArea, String authors, boolean visible, String visibilityReason, String filename, FileType fileType, int downloadCount, Date updatedAt, User uploadedBy) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.scientificArea = scientificArea;
        this.authors = authors;
        this.visible = visible;
        this.visibilityReason = visibilityReason;
        this.filename = filename;
        this.fileType = fileType;
        this.downloadCount = downloadCount;
        this.updatedAt = updatedAt;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getScientificArea() {
        return scientificArea;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getVisibilityReason() {
        return visibilityReason;
    }

    public String getFilename() {
        return filename;
    }

    public FileType getFileType() {
        return fileType;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public List<PublicationHistory> getHistory() {
        return history;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public void setScientificArea(String scientificArea) {
        this.scientificArea = scientificArea;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setVisibilityReason(String visibilityReason) {
        this.visibilityReason = visibilityReason;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public void setHistory(List<PublicationHistory> history) {
        this.history = history;
    }

    //metodos auxiliares


    public void addTag(Tag tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
            if (!tag.getPublications().contains(this)) {
                tag.getPublications().add(this);
            }
        }
    }

    public void removeTag(Tag tag) {
        if (this.tags.remove(tag)) {
            tag.getPublications().remove(this);
        }
    }

    public void addComment(Comment c) {
        c.setPublication(this);
        this.comments.add(c);
    }

    public void removeComment(Comment c) {
        this.comments.remove(c);
        c.setPublication(null);
    }

    public void addOrUpdateRating(Rating r) {
        r.setPublication(this);
        // substitui rating do mesmo user, se existir
        this.ratings.removeIf(x -> x.getUser().equals(r.getUser()));
        this.ratings.add(r);
    }

    public void removeRatingByUser(User u) {
        this.ratings.removeIf(x -> x.getUser().equals(u));
    }

    public void changeVisibility(boolean visible, String reason) {
        this.visible = visible;
        this.visibilityReason = reason;
        edit();
    }

    public void edit() {
        this.updatedAt = new Date();
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}
