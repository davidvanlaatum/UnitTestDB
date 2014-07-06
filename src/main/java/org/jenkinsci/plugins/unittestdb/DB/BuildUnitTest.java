package org.jenkinsci.plugins.unittestdb.DB;

import hudson.Extension;
import java.io.Serializable;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author David van Laatum
 */
@Entity
@Extension
@Table ( name = "build_unit_tests" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "BuildUnitTest.findAll", query
                = "SELECT b FROM BuildUnitTest b" ),
  @NamedQuery ( name = "BuildUnitTest.findByBuildUnitTestId", query
                = "SELECT b FROM BuildUnitTest b WHERE b.buildUnitTestId = :buildUnitTestId" ),
  @NamedQuery ( name = "BuildUnitTest.findByDuration", query
                = "SELECT b FROM BuildUnitTest b WHERE b.duration = :duration" ),
  @NamedQuery ( name = "BuildUnitTest.findByState", query
                = "SELECT b FROM BuildUnitTest b WHERE b.state = :state" ),
  @NamedQuery ( name = "BuildUnitTest.findByExecutor", query
                = "SELECT b FROM BuildUnitTest b WHERE b.executor = :executor" ) } )
public class BuildUnitTest extends DBObject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "build_unit_test_id" )
  private Integer buildUnitTestId;
  // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
  @Column ( name = "duration" )
  private Double duration;
  @Lob
  @Column ( name = "errordetails" )
  private String errordetails;
  @Lob
  @Column ( name = "errorstack" )
  private String errorstack;
  @Basic ( optional = false )
  @Column ( name = "state" )
  @Enumerated ( EnumType.STRING )
  private UnitTestState state;
  @Column ( name = "executor" )
  private Integer executor;
  @JoinColumn ( name = "node_id", referencedColumnName = "node_id" )
  @ManyToOne
  private Node node;
  @JoinColumn ( name = "job_id", referencedColumnName = "job_id" )
  @ManyToOne ( optional = false )
  private Job job;
  @JoinColumn ( name = "build_id", referencedColumnName = "build_id" )
  @ManyToOne ( optional = false )
  private Build build;
  @JoinColumn ( name = "unit_test_id", referencedColumnName = "unit_test_id" )
  @ManyToOne ( optional = false )
  private UnitTest unitTest;

  public BuildUnitTest () {
  }

  public BuildUnitTest ( Integer buildUnitTestId ) {
    this.buildUnitTestId = buildUnitTestId;
  }

  public BuildUnitTest ( Integer buildUnitTestId, UnitTestState state ) {
    this.buildUnitTestId = buildUnitTestId;
    this.state = state;
  }

  public Integer getBuildUnitTestId () {
    return buildUnitTestId;
  }

  public void setBuildUnitTestId ( Integer buildUnitTestId ) {
    this.buildUnitTestId = buildUnitTestId;
  }

  public Double getDuration () {
    return duration;
  }

  public void setDuration ( Double duration ) {
    this.duration = duration;
  }

  public String getErrordetails () {
    return errordetails;
  }

  public void setErrordetails ( String errordetails ) {
    this.errordetails = errordetails;
  }

  public String getErrorstack () {
    return errorstack;
  }

  public void setErrorstack ( String errorstack ) {
    this.errorstack = errorstack;
  }

  public UnitTestState getState () {
    return state;
  }

  public void setState ( UnitTestState state ) {
    this.state = state;
  }

  public Integer getExecutor () {
    return executor;
  }

  public void setExecutor ( Integer executor ) {
    this.executor = executor;
  }

  public Node getNode () {
    return node;
  }

  public void setNode ( Node node ) {
    this.node = node;
  }

  public Job getJob () {
    return job;
  }

  public void setJob ( Job job ) {
    this.job = job;
  }

  public Build getBuild () {
    return build;
  }

  public void setBuild ( Build build ) {
    this.build = build;
  }

  public UnitTest getUnitTest () {
    return unitTest;
  }

  public void setUnitTest ( UnitTest unitTest ) {
    this.unitTest = unitTest;
  }

  @Override
  public int hashCode () {
    int hash = 0;
    hash += ( buildUnitTestId != null ? buildUnitTestId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof BuildUnitTest ) ) {
      return false;
    }
    BuildUnitTest other = (BuildUnitTest) object;
    if ( ( this.buildUnitTestId == null && other.buildUnitTestId != null )
                 || ( this.buildUnitTestId != null && !this.buildUnitTestId
                     .equals (
                     other.buildUnitTestId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.BuildUnitTest[ buildUnitTestId="
                   + buildUnitTestId + " ]";
  }

}
