// src/main/java/pt/ipleiria/estg/dei/ei/dae/academics/ejbs/ConfigBean.java
package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.Role;

import java.util.List;

@Singleton
@Startup
public class ConfigBean {

    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    private String uploadsDir;


    public String getUploadsDir() {
        return uploadsDir;
    }

    @PostConstruct
    public void init() {
        try {
            // não repete seed se já existir pelo menos um user
            this.uploadsDir = System.getProperty("user.home") + "/academics_uploads";
            List<User> existing = userBean.getAll();
            if (existing != null && !existing.isEmpty()) {
                System.out.println("ConfigBean: dados já existem, não é necessário popular.");
                return;
            }

            User admin = userBean.create(
                    "admin",
                    "admin@academics.pt",
                    "admin123",
                    Role.ADMIN
            );

            User manager = userBean.create(
                    "manager",
                    "manager@academics.pt",
                    "manager123",
                    Role.MANAGER
            );

            User contributor = userBean.create(
                    "contributor",
                    "contributor@academics.pt",
                    "contrib123",
                    Role.CONTRIBUTOR
            );

            Long adminId = admin.getId();
            Long managerId = manager.getId();
            Long contributorId = contributor.getId();

            publicationBean.create(
                    "First Publication",
                    "Short summary of the first publication.",
                    "Artificial Intelligence",
                    "Ana Silva; Carlos Mendes",
                    "paper1.pdf",
                    FileType.PDF,
                    contributorId
            );

            publicationBean.create(
                    "Second Publication",
                    "Second seeded publication.",
                    "Software Engineering",
                    "João Santos",
                    "paper2.pdf",
                    FileType.PDF,
                    managerId
            );

            System.out.println("ConfigBean: dados iniciais criados com sucesso.");

        } catch (Exception e) {
            System.out.println("ConfigBean: erro ao popular BD -> " + e.getMessage());
        }
    }
}
