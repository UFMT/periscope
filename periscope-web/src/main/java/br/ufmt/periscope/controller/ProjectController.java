package br.ufmt.periscope.controller;

import br.ufmt.periscope.indexer.PatentIndexer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.qualifier.LoggedUser;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.repository.ProjectRepository;

import com.github.jmkgreen.morphia.Datastore;
import java.net.UnknownHostException;

/**
 * - @ManagedBean<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações relacionadas aos projetos
 */
@ManagedBean
@ViewScoped
public class ProjectController {

    private @Inject
    Datastore ds;
    private @Inject
    ProjectRepository projectRepository;
    private @Inject
    PatentRepository patentRepository;
    private @Inject
    @LoggedUser
    User currentUser;
    private @Inject
    PatentIndexer indexer;
    private DataModel<Project> projects = null;
    private Project project = new Project();
    private List<User> freeUsers = null;
    private String selectedUser = null;
    private boolean editing = false;

    /**
     * Método pós construtor do controller, usado apenas para verificar se está na página de adição ou edição de projetos
     */
    @PostConstruct
    public void init() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        if (req.getParameter("projectId") != null) {
            editing = true;
            project = ds.get(Project.class, new ObjectId(req.getParameter("projectId")));

        }
    }

    /**
     * Adiciona novo usuário ao projeto como observador
     */
    public void adduser() {

        if (selectedUser != null && selectedUser.trim().length() > 0) {

            ObjectId key = new ObjectId(selectedUser);
            User user = ds.get(User.class, key);
            if (user != null) {

                this.project.getObservers().add(user);
                freeUsers = null;
            }
        }
    }

    /**
     * Remove um usuário dos observadores de um projeto
     * @param id identificador de um usuário
     */
    public void removeUser(String id) {
        User user = null;
        for (User u : project.getObservers()) {
            if (u.getId().toString().contentEquals(id)) {
                user = u;
                freeUsers = null;
                break;
            }
        }
        if (user != null) {
            this.project.getObservers().remove(user);
            selectedUser = null;
        }
    }

    /**
     * Cria um novo projeto
     * @return página de listagem de projetos
     */
    public String create() {
        project.setCreatedAt(new Date());
        project.setUpdateAt(new Date());
        project.setOwner(getCurrentUser());
        ds.save(project);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("success", "Criado com Sucesso");
        return "projectList";
    }

    /**
     * Salva um projeto que foi editado
     * @return página de listagem de projetos
     */
    public String save() {
        project.setUpdateAt(new Date());
        ds.save(project);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Atualizado com Sucesso");
        return "projectList";
    }

    /**
     * Deleta um projeto
     * @param id identificador de um projeto
     * @return página de listagem de projetos
     * @throws UnknownHostException
     */
    public String delete(String id) throws UnknownHostException {
        projectRepository.deleteProject(id);
        Flash flash = FacesContext.getCurrentInstance().
                getExternalContext().getFlash();
        flash.put("info", "Deletado com Sucesso");
        return "projectList";
    }

    /**
     * Reindexa os dados do projeto
     * @param project projeto existente no sistema
     * @return <code>""</code>
     */
    public String reindexAll(Project project) {
        indexer.reindex(project);
        return "";
    }

    /**
     * Método responsávle por pegar os projetos de um usuário
     * @return lista com projetos de um determinado usuário
     */
    public DataModel<Project> getProjects() {
        if (projects == null) {
            projects = new ListDataModel<Project>(projectRepository.getProjectList(currentUser));
        }
        return projects;
    }

    /**
     *
     * @param projects
     */
    public void setProjects(DataModel<Project> projects) {
        this.projects = projects;
    }

    /**
     *
     * @return
     */
    public Project getProject() {
        return project;
    }

    /**
     *
     * @param project
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Método responsável por listar os usuários não vinculados a um determinado projeto
     * @return lista de usuários que não estão vinculados a um determinado projeto
     */
    public List<User> getFreeUsers() {
        if (freeUsers == null) {
            List<ObjectId> keys = new ArrayList<ObjectId>();
            keys.add(getCurrentUser().getId());
            for (User u : project.getObservers()) {
                keys.add(u.getId());
            }
            freeUsers = ds.createQuery(User.class).field("id").notIn(keys).asList();
        }
        return freeUsers;
    }

    /**
     *
     * @param freeUsers
     */
    public void setFreeUsers(List<User> freeUsers) {
        this.freeUsers = freeUsers;
    }

    /**
     *
     * @return
     */
    public String getSelectedUser() {
        return selectedUser;
    }

    /**
     *
     * @param selectedUser
     */
    public void setSelectedUser(String selectedUser) {
        this.selectedUser = selectedUser;
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
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     *
     * @param currentUser
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

}
