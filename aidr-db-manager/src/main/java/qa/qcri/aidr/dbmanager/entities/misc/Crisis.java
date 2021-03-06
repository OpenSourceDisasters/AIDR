/**
 * Implements the entity corresponding to the crisis table in the aidr_predict DB
 * 
 * @author Koushik
 */
package qa.qcri.aidr.dbmanager.entities.misc;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.Hibernate;

import qa.qcri.aidr.dbmanager.entities.model.ModelFamily;
import qa.qcri.aidr.dbmanager.entities.model.NominalAttribute;
import qa.qcri.aidr.dbmanager.entities.task.Document;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "crisis", catalog = "aidr_predict", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Crisis implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7692349620189189978L;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "crisisID", unique = true, nullable = false)
	private Long crisisId;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userID", nullable = false)
	@JsonBackReference
	private Users users;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "crisisTypeID", nullable = false)
	@JsonBackReference
	private CrisisType crisisType;
	
	@Column(name = "name", nullable = false, length = 140)
	private String name;
	
	@Column(name = "code", unique = true, nullable = false, length = 64)
	private String code;
	
	@Column(name = "isTrashed", nullable = false)
	private boolean isTrashed;
	
	@Column(name = "isMicromapperEnabled", nullable = false)
	private boolean isMicromapperEnabled;
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "crisis_nominal_attribute", catalog = "aidr_predict", joinColumns = { @JoinColumn(name = "crisisID", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "nominalAttributeID", nullable = false, updatable = false) })
	@JsonManagedReference
	private List<NominalAttribute> nominalAttributes;
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "crisis")
	@JsonManagedReference
	private List<Document> documents;
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "crisis")
	@JsonManagedReference
	private List<ModelFamily> modelFamilies;

	public Crisis() {
	}

	/*public Crisis(Users users, CrisisType crisisType, String name, String code,
			boolean isTrashed) {
		this.users = users;
		this.crisisType = crisisType;
		this.name = name;
		this.code = code;
		this.isTrashed = isTrashed;
	}*/
	
	public Crisis(Users users, CrisisType crisisType, String name, String code,
			boolean isTrashed, boolean isMicromapperEnabled) {
		this.users = users;
		this.crisisType = crisisType;
		this.name = name;
		this.code = code;
		this.isTrashed = isTrashed;
		this.isMicromapperEnabled = isMicromapperEnabled;
	}

	public Crisis(Users users, CrisisType crisisType, String name, String code,
			boolean isTrashed, boolean isMicromapperEnabled, List<NominalAttribute> nominalAttributes, List<Document> documents,
			List<ModelFamily> modelFamilies) {
		this.users = users;
		this.crisisType = crisisType;
		this.name = name;
		this.code = code;
		this.isTrashed = isTrashed;
		this.isMicromapperEnabled = isMicromapperEnabled;
		this.nominalAttributes = nominalAttributes;
		this.documents = documents;
		this.modelFamilies = modelFamilies;
	}


	public Long getCrisisId() {
		return this.crisisId;
	}

	public void setCrisisId(Long crisisId) {
		this.crisisId = crisisId;
	}

	public Users getUsers() {
		return this.users;
	}

	public void setUsers(Users users) {
		this.users = users;
	}

	
	public CrisisType getCrisisType() {
		return this.crisisType;
	}

	public void setCrisisType(CrisisType crisisType) {
		this.crisisType = crisisType;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public boolean isIsTrashed() {
		return this.isTrashed;
	}

	public void setIsTrashed(boolean isTrashed) {
		this.isTrashed = isTrashed;
	}
	
	public boolean isIsMicromapperEnabled() {
		return isMicromapperEnabled;
	}

	public void setIsMicromapperEnabled(boolean isMicromapperEnabled) {
		this.isMicromapperEnabled = isMicromapperEnabled;
	}
	
	public List<NominalAttribute> getNominalAttributes() {
		return this.nominalAttributes;
	}

	public void setNominalAttributes(List<NominalAttribute> nominalAttributes) {
		this.nominalAttributes = nominalAttributes;
	}

	public List<Document> getDocuments() {
		return this.documents;
	}

	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}

	public List<ModelFamily> getModelFamilies() {
		return this.modelFamilies;
	}

	public void setModelFamilies(List<ModelFamily> modelFamilies) {
		this.modelFamilies = modelFamilies;
	}
	
	public boolean hasUsers() {
		return Hibernate.isInitialized(this.users);
	}
	
	public boolean hasCrisisType() {
		return Hibernate.isInitialized(this.crisisType);
	}
	
	public boolean hasDocuments() {
		//return ((PersistentList) this.documents).wasInitialized();
		return Hibernate.isInitialized(this.documents);
	}
	
	public boolean hasNominalAttributes() {
		//return ((PersistentList) this.nominalAttributes).wasInitialized();
		return Hibernate.isInitialized(this.nominalAttributes);
	}
	
	public boolean hasModelFamilies() {
		//return ((PersistentList) this.modelFamilies).wasInitialized();
		return Hibernate.isInitialized(this.modelFamilies);
	}
}
