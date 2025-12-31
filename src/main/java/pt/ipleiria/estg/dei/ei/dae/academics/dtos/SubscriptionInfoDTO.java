package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

public class SubscriptionInfoDTO {
    private Long id;
    private String name;
    private String subscribedAt;
    private int publicationsCount;
    private int newPublicationsSinceLastVisit;

    public SubscriptionInfoDTO() {
    }

    public SubscriptionInfoDTO(Long id, String name, String subscribedAt,
                               int publicationsCount, int newPublicationsSinceLastVisit) {
        this.id = id;
        this.name = name;
        this.subscribedAt = subscribedAt;
        this.publicationsCount = publicationsCount;
        this.newPublicationsSinceLastVisit = newPublicationsSinceLastVisit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(String subscribedAt) { this.subscribedAt = subscribedAt; }

    public int getPublicationsCount() { return publicationsCount; }
    public void setPublicationsCount(int publicationsCount) { this.publicationsCount = publicationsCount; }

    public int getNewPublicationsSinceLastVisit() { return newPublicationsSinceLastVisit; }
    public void setNewPublicationsSinceLastVisit(int newPublicationsSinceLastVisit) {
        this.newPublicationsSinceLastVisit = newPublicationsSinceLastVisit;
    }
}
