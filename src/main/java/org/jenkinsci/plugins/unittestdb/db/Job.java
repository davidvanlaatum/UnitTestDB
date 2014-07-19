package org.jenkinsci.plugins.unittestdb.db;

import com.google.common.base.Strings;
import hudson.Extension;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
@Table ( name = "jobs" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "Job.findAll", query
                = "SELECT j FROM Job j" ),
  @NamedQuery ( name = "Job.findByJobId", query
                = "SELECT j FROM Job j WHERE j.jobId = :jobId" ),
  @NamedQuery ( name = "Job.findByName", query
                = "SELECT j FROM Job j WHERE j.name = :name" ),
  @NamedQuery ( name = "Job.findByLastrun", query
                = "SELECT j FROM Job j WHERE j.lastrun = :lastrun" ) } )
public class Job extends DBObject implements Serializable {

  private static final Logger LOG = Logger.getLogger ( Job.class.getName () );
  private static final Object lock = new Object ();

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "job_id" )
  private Integer jobId;
  @Basic ( optional = false )
  @Column ( name = "name", unique = true )
  private String name;
  @Column ( name = "lastrun" )
  @Temporal ( TemporalType.TIMESTAMP )
  private Date lastrun;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "job" )
  private List<Failure> failureList;
  @JoinColumn ( name = "last_build_id", referencedColumnName = "build_id" )
  @ManyToOne
  private Build lastBuild;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "job" )
  private List<Build> buildList;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "job" )
  private List<UnitTest> unitTestList;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "job" )
  private List<BuildUnitTest> buildUnitTestList;

  public Job () {
  }

  public Job ( Integer jobId ) {
    this.jobId = jobId;
  }

  public Job ( Integer jobId, String name ) {
    this.jobId = jobId;
    this.name = name;
  }

  public Integer getJobId () {
    return jobId;
  }

  public void setJobId ( Integer jobId ) {
    this.jobId = jobId;
  }

  public String getName () {
    return name;
  }

  public void setName ( String name ) {
    this.name = name;
  }

  public Date getLastrun () {
    return lastrun;
  }

  public void setLastrun ( Date lastrun ) {
    this.lastrun = lastrun;
  }

  @XmlTransient
  public List<Failure> getFailureList () {
    return failureList;
  }

  public void setFailureList ( List<Failure> failureList ) {
    this.failureList = failureList;
  }

  public Build getLastBuild () {
    return lastBuild;
  }

  public void setLastBuild ( Build lastBuild ) {
    this.lastBuild = lastBuild;
  }

  @XmlTransient
  public List<Build> getBuildList () {
    return buildList;
  }

  public void setBuildList ( List<Build> buildList ) {
    this.buildList = buildList;
  }

  @XmlTransient
  public List<UnitTest> getUnitTestList () {
    return unitTestList;
  }

  public void setUnitTestList ( List<UnitTest> unitTestList ) {
    this.unitTestList = unitTestList;
  }

  @XmlTransient
  public List<BuildUnitTest> getBuildUnitTestList () {
    return buildUnitTestList;
  }

  public void setBuildUnitTestList ( List<BuildUnitTest> buildUnitTestList ) {
    this.buildUnitTestList = buildUnitTestList;
  }

  @Override
  public int hashCode () {
    int hash = 0;
    hash += ( jobId != null ? jobId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof Job ) ) {
      return false;
    }
    Job other = (Job) object;
    if ( ( this.jobId == null && other.jobId != null ) || ( this.jobId != null
                                                            && !this.jobId
                                                           .equals ( other.jobId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.Job[ jobId=" + jobId + " ]";
  }

  public static Job findByName ( String name, EntityManager em, boolean create ) {
    requireNonNull ( em, "No EntityManager passed in" );
    if ( Strings.isNullOrEmpty ( name ) ) {
      throw new IllegalArgumentException ( "Name is null or empty" );
    }
    Query q = em.createNamedQuery ( "Job.findByName" );
    q.setParameter ( "name", name );
    q.setMaxResults ( 1 );
    Job rt = null;
    try {
      rt = (Job) q.getSingleResult ();
    } catch ( NoResultException ex ) {
      if ( create ) {
        if ( em.getTransaction ().isActive () ) {
          throw new IllegalStateException ( "Already in a transaction" );
        }
        synchronized ( lock ) {
          try {
            em.getTransaction ().begin ();
            Createlocks lock = Createlocks.getLockObject ( em, "job" );
            em.lock ( lock, LockModeType.PESSIMISTIC_WRITE );
            rt = findByName ( name, em, false );
            if ( rt == null ) {
              LOG.log ( Level.INFO, "Creating job {0}", name );
              rt = new Job ();
              rt.setName ( name );
              em.persist ( rt );
            }
            em.getTransaction ().commit ();
          } catch ( Exception ex2 ) {
            em.getTransaction ().rollback ();
            LOG.log ( Level.SEVERE, null, ex2 );
            throw ex2;
          }
        }
      }
    }

    if ( create ) {
      requireNonNull ( rt, "Somehow with create on we still returned null" );
    }

    if ( rt != null && !em.contains ( rt ) ) {
      throw new IllegalStateException (
              "Somehow the job we are returning is not in the entity manager" );
    }

    return rt;
  }

}
