package pt.ipleiria.estg.dei.ei.dae.academics.enums;

public enum Role {

    //esta acabado

    /**
     * Colaborador: Investigadores, programadores, estudantes
     * Podem fazer upload, comentar, rating, subscrever tags
     */

    CONTRIBUTOR,

    /**
     * Responsável: Investigadores sénior, coordenadores
     * Podem criar/remover tags, ocultar comentários e publicações
     */

    MANAGER,

    /**
     * Administrador: Gestores do sistema
     * Podem gerir utilizadores, alterar roles, consultar todo o histórico
     */

    ADMIN
}
