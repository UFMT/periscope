package br.ufmt.periscope.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Reference;

@Entity
public class Project implements Serializable{
	
	@Id
	private ObjectId id;
	
	private String title;
	private String description;
	private Boolean isPublic = false;
	
	private Date createdAt;
	private Date updateAt;
	
	@Reference private User owner;
	@Reference private Set<User> observers = new HashSet<User>();
	
	@Reference private Set<Patent> patents = new HashSet<Patent>();

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdateAt() {
		return updateAt;
	}

	public void setUpdateAt(Date updateAt) {
		this.updateAt = updateAt;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public Set<User> getObservers() {
		return observers;
	}

	public void setObservers(Set<User> observers) {
		this.observers = observers;
	}

	public Set<Patent> getPatents() {
		return patents;
	}

	public void setPatents(Set<Patent> patents) {
		this.patents = patents;
	}
	
}
