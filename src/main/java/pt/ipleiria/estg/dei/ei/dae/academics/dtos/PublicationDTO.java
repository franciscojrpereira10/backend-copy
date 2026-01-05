package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PublicationDTO {

    private Long id;
    private String title;
    private String authors;
    private String scientificArea;
    private String summary;
    private boolean visible;
    private String visibilityReason;
    private String filename;
    private FileType fileType;
    private int downloadCount;
    private Date createdAt;
    private Date updatedAt;
    private Long uploadedByUserId;
    private String uploadedByName;
    private List<TagDTO> tags;
    private int commentCount;
    private double averageRating;
    private int ratingsCount;

    public PublicationDTO() {
        this.tags = new ArrayList<>();
    }

    public PublicationDTO(Long id, String title, String authors, String scientificArea,
                          String summary, boolean visible, String visibilityReason,
                          String filename, FileType fileType, int downloadCount,
                          Date createdAt, Date updatedAt, Long uploadedByUserId,
                          String uploadedByName, List<TagDTO> tags, int commentCount,
                          double averageRating, int ratingsCount) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.scientificArea = scientificArea;
        this.summary = summary;
        this.visible = visible;
        this.visibilityReason = visibilityReason;
        this.filename = filename;
        this.fileType = fileType;
        this.downloadCount = downloadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedByName = uploadedByName;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.commentCount = commentCount;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
    }

    // ===== getters & setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getScientificArea() { return scientificArea; }
    public void setScientificArea(String scientificArea) { this.scientificArea = scientificArea; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public String getVisibilityReason() { return visibilityReason; }
    public void setVisibilityReason(String visibilityReason) { this.visibilityReason = visibilityReason; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Long getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(Long uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }

    public List<TagDTO> getTags() { return tags; }
    public void setTags(List<TagDTO> tags) {
        this.tags = (tags != null) ? tags : new ArrayList<>();
    }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(int ratingsCount) { this.ratingsCount = ratingsCount; }

    // ===== Conversões =====

    // Versão base, sem coleções LAZY
    public static PublicationDTO from(Publication publication) {
        PublicationDTO dto = new PublicationDTO();
        dto.setId(publication.getId());
        dto.setTitle(publication.getTitle());
        dto.setAuthors(publication.getAuthors());
        dto.setScientificArea(publication.getScientificArea());
        dto.setSummary(publication.getSummary());
        dto.setVisible(publication.isVisible());
        dto.setVisibilityReason(publication.getVisibilityReason());
        dto.setFilename(publication.getFilename());
        dto.setFileType(publication.getFileType());
        dto.setDownloadCount(publication.getDownloadCount());
        dto.setCreatedAt(publication.getCreatedAt());
        dto.setUpdatedAt(publication.getUpdatedAt());

        if (publication.getUploadedBy() != null) {
            dto.setUploadedByUserId(publication.getUploadedBy().getId());
            dto.setUploadedByName(publication.getUploadedBy().getUsername());
        }

        // valores por omissão; serão preenchidos pelo PublicationBean.toDetailedDTO
        dto.setTags(new ArrayList<>());
        dto.setCommentCount(0);
        dto.setAverageRating(0.0);
        dto.setRatingsCount(0);

        return dto;
    }

    public static List<PublicationDTO> from(List<Publication> publications) {
        return publications.stream()
                .map(PublicationDTO::from)
                .toList();
    }
}
