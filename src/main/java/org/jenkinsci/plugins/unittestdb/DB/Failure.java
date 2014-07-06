package org.jenkinsci.plugins.unittestdb.DB;

import hudson.Extension;
import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author David van Laatum
 */
@Entity
@Extension
@Table ( name = "failures" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "Failure.findAll", query
                = "SELECT f FROM Failure f" ),
  @NamedQuery ( name = "Failure.findByFailureId", query
                = "SELECT f FROM Failure f WHERE f.failureId = :failureId" ),
  @NamedQuery ( name = "Failure.findByStateAndJob", query
                = "SELECT f FROM Failure f WHERE f.state = :state AND f.job.jobId = :job" ) } )
public class Failure extends DBObject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "failure_id" )
  private Integer failureId;
  @Basic ( optional = false )
  @Column ( name = "state" )
  @Enumerated ( EnumType.STRING )
  private FailureState state;
  @JoinColumn ( name = "job_id", referencedColumnName = "job_id" )
  @ManyToOne ( optional = false )
  private Job job;
  @JoinColumn ( name = "last_build", referencedColumnName = "build_id" )
  @ManyToOne ( optional = false )
  private Build lastBuild;
  @JoinColumn ( name = "first_build", referencedColumnName = "build_id" )
  @ManyToOne ( optional = false )
  private Build firstBuild;
  @JoinColumn ( name = "unit_test_id", referencedColumnName = "unit_test_id" )
  @ManyToOne ( optional = false )
  private UnitTest unitTest;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "failure" )
  private List<FailureUser> users;

  public Failure () {
  }

  public Failure ( Integer failureId ) {
    this.failureId = failureId;
  }

  public Failure ( Integer failureId, FailureState state ) {
    this.failureId = failureId;
    this.state = state;
  }

  public Integer getFailureId () {
    return failureId;
  }

  public void setFailureId ( Integer failureId ) {
    this.failureId = failureId;
  }

  public FailureState getState () {
    return state;
  }

  public void setState ( FailureState state ) {
    this.state = state;
  }

  public Job getJob () {
    return job;
  }

  public void setJob ( Job job ) {
    this.job = job;
  }

  public Build getFirstBuild () {
    return firstBuild;
  }

  public void setFirstBuild ( Build firstBuild ) {
    this.firstBuild = firstBuild;
  }

  public Build getLastBuild () {
    return lastBuild;
  }

  public void setLastBuild ( Build lastBuild ) {
    this.lastBuild = lastBuild;
  }

  public UnitTest getUnitTest () {
    return unitTest;
  }

  public void setUnitTest ( UnitTest unitTest ) {
    this.unitTest = unitTest;
  }

  @XmlTransient
  public List<FailureUser> getUsers () {
    return users;
  }

  public void setUsers ( List<FailureUser> users ) {
    this.users = users;
  }

  @Override
  public int hashCode () {
    int hash = 0;
    hash += ( failureId != null ? failureId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof Failure ) ) {
      return false;
    }
    Failure other = (Failure) object;
    if ( ( this.failureId == null && other.failureId != null )
                 || ( this.failureId != null && !this.failureId
                     .equals ( other.failureId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.Failure[ failureId=" + failureId + " ]";
  }

  @SuppressWarnings ( "unchecked" )
  public static SortedMap<Integer, Failure> findByJob ( Job job,
                                                        EntityManager em ) {
    requireNonNull ( em, "No EntityManager passed in" );
    requireNonNull ( job, "No job passwd in" );
    Query q = em.createNamedQuery ( "Failure.findByStateAndJob" );
    q.setParameter ( "job", job.getJobId () );
    q.setParameter ( "state", FailureState.Failed );
    SortedMap<Integer, Failure> rt = new TreeMap<> ();
    try {
      for ( Failure f : (List<Failure>) q.getResultList () ) {
        rt.put ( f.getUnitTest ().getUnitTestId (), f );
      }
    } catch ( NoResultException ex ) {
    }

    return rt;
  }

}
