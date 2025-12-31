package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import java.io.Serializable;

public class ChangePasswordDTO implements Serializable {
    public String oldPassword;
    public String newPassword;

    public ChangePasswordDTO() { }
}
