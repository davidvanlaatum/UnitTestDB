package org.jenkinsci.plugins.unittestdb.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import hudson.Extension;
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
@Table ( name = "builds" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "Build.findAll", query
                = "SELECT b FROM Build b" ),
  @NamedQuery ( name = "Build.findByBuildId", query
                = "SELECT b FROM Build b WHERE b.buildId = :buildId" ),
  @NamedQuery ( name = "Build.findByJobAndJenkinsID", query
                = "SELECT b FROM Build b WHERE b.job.jobId = :job AND b.jenkinsId = :jenkinsid" ),
  @NamedQuery ( name = "Build.findAllNeedingStats", query
                = "SELECT b FROM Build b WHERE b.tests IS NULL OR b.skipped IS NULL OR b.skipped IS NULL" )
} )
public class Build extends DBObject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "build_id" )
  private Integer buildId;
  @Basic ( optional = false )
  @Column ( name = "jenkins_id" )
  private int jenkinsId;
  @Basic ( optional = false )
  @Column ( name = "time" )
  @Temporal ( TemporalType.TIMESTAMP )
  private Date time;
  @Column ( name = "tests" )
  private Integer tests;
  @Column ( name = "failures" )
  private Integer failures;
  @Column ( name = "skipped" )
  private Integer skipped;
  @OneToMany ( mappedBy = "lastBuild" )
  private List<Job> jobList;
  @JoinColumn ( name = "job_id", referencedColumnName = "job_id" )
  @ManyToOne ( optional = false )
  private Job job;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "build" )
  private List<BuildUnitTest> buildUnitTestList;

  public Build () {
  }

  public Build ( Integer buildId ) {
    this.buildId = buildId;
  }

  public Build ( Integer buildId, int jenkinsId, Date time ) {
    this.buildId = buildId;
    this.jenkinsId = jenkinsId;
    this.time = time;
  }

  public Integer getBuildId () {
    return buildId;
  }

  public void setBuildId ( Integer buildId ) {
    this.buildId = buildId;
  }

  public int getJenkinsId () {
    return jenkinsId;
  }

  public void setJenkinsId ( int jenkinsId ) {
    this.jenkinsId = jenkinsId;
  }

  public Date getTime () {
    return time;
  }

  public void setTime ( Date time ) {
    this.time = time;
  }

  public Integer getTests () {
    return tests;
  }

  public void setTests ( Integer tests ) {
    this.tests = tests;
  }

  public Integer getFailures () {
    return failures;
  }

  public void setFailures ( Integer failures ) {
    this.failures = failures;
  }

  public Integer getSkipped () {
    return skipped;
  }

  public void setSkipped ( Integer skipped ) {
    this.skipped = skipped;
  }

  @XmlTransient
  public List<Job> getJobList () {
    return jobList;
  }

  public void setJobList ( List<Job> jobList ) {
    this.jobList = jobList;
  }

  public Job getJob () {
    return job;
  }

  public void setJob ( Job job ) {
    this.job = job;
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
    hash += ( buildId != null ? buildId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof Build ) ) {
      return false;
    }
    Build other = (Build) object;
    if ( ( this.buildId == null && other.buildId != null ) || ( this.buildId
                                                                != null
                                                                && !this.buildId
                                                               .equals (
                                                               other.buildId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.Build[ buildId=" + buildId + " ]";
  }

  public static Build findByJobAndJenkinsID ( Job job, Integer jenkinsid,
                                              Date time, EntityManager em,
                                              boolean create ) {
    requireNonNull ( em, "No EntityManager passed in" );
    requireNonNull ( job, "No job passed in" );
    requireNonNull ( jenkinsid, "No jenkins id passed in" );
    Query q = em.createNamedQuery ( "Build.findByJobAndJenkinsID" );
    q.setParameter ( "job", job.getJobId () );
    q.setParameter ( "jenkinsid", jenkinsid );
    q.setMaxResults ( 1 );
    Build rt = null;
    try {
      rt = (Build) q.getSingleResult ();
    } catch ( NoResultException ex ) {
      if ( create ) {
        em.getTransaction ().begin ();
        em.lock ( job, LockModeType.PESSIMISTIC_WRITE );
        rt = findByJobAndJenkinsID ( job, jenkinsid, null, em, false );
        if ( rt == null ) {
          rt = new Build ();
          rt.setJob ( job );
          rt.setJenkinsId ( jenkinsid );
          rt.setTime ( time );
          try {
            em.persist ( rt );
            em.getTransaction ().commit ();
          } catch ( EntityExistsException ex2 ) {
            em.getTransaction ().rollback ();
            rt = findByJobAndJenkinsID ( job, jenkinsid, time, em, false );
            if ( rt == null ) {
              throw ex2;
            }
          } catch ( Exception ex2 ) {
            em.getTransaction ().rollback ();
            throw ex2;
          }
        } else {
          em.getTransaction ().commit ();
        }
      }
    }

    return rt;
  }

  @SuppressWarnings ( "unchecked" )
  public static List<Build> findAllNeedingStats ( EntityManager em ) {
    List<Build> rt = null;
    requireNonNull ( em, "No EntityManager passed in" );
    Query q = em.createNamedQuery ( "Build.findAllNeedingStats" );
    try {
      rt = q.getResultList ();
    } catch ( NoResultException ex ) {

    }
    return rt;
  }

}
