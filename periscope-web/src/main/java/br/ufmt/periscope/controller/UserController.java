package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.qualifier.LoggedUser;

import com.github.jmkgreen.morphia.Datastore;

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações elacionadas aos usuários do projeto
 */
@ManagedBean
@ViewScoped
public class UserController {

    private @Inject
    Datastore ds;
    private @Inject
    @LoggedUser
    User loggedUser;
    private DataModel<User> users = null;
    private User user = new User();
    private boolean editing = false;
    private List<UserLevel> levels = new ArrayList<UserLevel>();

    /**
     * Método de pós construção da classe que verifica se está editando o usuário e colocando seu nível de accesso
     */
    @PostConstruct
    public void init() {

//		for(UserLevel ul : UserLevel.values()){
//			if(ul.getAccessLevel() <= loggedUser.getUserLevel().getAccessLevel())
//				levels.add(ul);
//		}
        levels.add(UserLevel.USER);
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        if (req.getParameter("userId") != null) {
            editing = true;
            user = ds.get(User.class, new ObjectId(req.getParameter("userId")));

        }
    }

    /**
     * Salva um usuário
     * @return Página de Lista de Usuários
     */
    public String save() {
        User existingUser = ds.find(User.class).field("username").equal(user.getUsername()).get();
        boolean hasUniqueUsername = false;
        if (editing) {
            if (existingUser == null) {
                hasUniqueUsername = true;
            } else {
                //é o proprio usuário
                if (existingUser.getId().toString().contentEquals(user.getId().toString())) {
                    hasUniqueUsername = true;
                }
            }
        } else {
            hasUniqueUsername = existingUser == null;
        }
        if (hasUniqueUsername) {
            ds.save(user);
            Flash flash = FacesContext.getCurrentInstance().
                    getExternalContext().getFlash();
            flash.put("success", "Salvo com Sucesso");
            return "userList";
        } else {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login deve ser único", "Login já existente");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return null;
        }

    }

    /**
     * Deleta um usuário
     * @param id Identificador do usuário
     * @return Página de Lista de Usuários
     */
    public String delete(String id) {
        ds.delete(User.class, new ObjectId(id));
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com sucesso");
        return "userList";
    }

    /**
     * Lista de usuários
     * @return Lista de usuários
     */
    public DataModel<User> getUsers() {
        if (users == null) {
            users = new ListDataModel<User>(ds.find(User.class).asList());
        }
        return users;
    }

    /**
     *
     * @param users
     */
    public void setUsers(DataModel<User> users) {
        this.users = users;
    }

    /**
     *
     * @return
     */
    public boolean isEditing() {
        return editing;
    }

    /**
     *
     * @param editing
     */
    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    /**
     *
     * @return
     */
    public User getUser() {
        return user;
    }

    /**
     *
     * @param user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     *
     * @return
     */
    public List<UserLevel> getLevels() {
        return levels;
    }

    /**
     *
     * @param levels
     */
    public void setLevels(List<UserLevel> levels) {
        this.levels = levels;
    }

}