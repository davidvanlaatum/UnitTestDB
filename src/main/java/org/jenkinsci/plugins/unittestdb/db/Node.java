package org.jenkinsci.plugins.unittestdb.db;

import com.google.common.base.Strings;
import hudson.Extension;
import java.io.Serializable;
import java.util.List;
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
@Table ( name = "nodes" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "Node.findAll", query
                = "SELECT n FROM Node n" ),
  @NamedQuery ( name = "Node.findByNodeId", query
                = "SELECT n FROM Node n WHERE n.nodeId = :nodeId" ),
  @NamedQuery ( name = "Node.findByName", query
                = "SELECT n FROM Node n WHERE n.name = :name" ) } )
public class Node extends DBObject implements Serializable {

  private static final Object lock = new Object ();

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "node_id" )
  private Integer nodeId;
  @Basic ( optional = false )
  @Column ( name = "name" )
  private String name;
  @OneToMany ( mappedBy = "node" )
  private List<BuildUnitTest> buildUnitTestList;

  public Node () {
  }

  public Node ( Integer nodeId ) {
    this.nodeId = nodeId;
  }

  public Node ( Integer nodeId, String name ) {
    this.nodeId = nodeId;
    this.name = name;
  }

  public Integer getNodeId () {
    return nodeId;
  }

  public void setNodeId ( Integer nodeId ) {
    this.nodeId = nodeId;
  }

  public String getName () {
    return name;
  }

  public void setName ( String name ) {
    this.name = name;
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
    hash += ( nodeId != null ? nodeId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof Node ) ) {
      return false;
    }
    Node other = (Node) object;
    if ( ( this.nodeId == null && other.nodeId != null ) || ( this.nodeId
                                                              != null
                                                              && !this.nodeId
                                                             .equals (
                                                             other.nodeId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.Node[ nodeId=" + nodeId + " ]";
  }

  public static Node findByName ( String name, EntityManager em,
                                  boolean create ) {
    requireNonNull ( em, "No EntityManager passed in" );
    requireNonNull ( name, "Name is null" );
    Query q = em.createNamedQuery ( "Node.findByName" );
    q.setParameter ( "name", name.isEmpty () ? "master" : name );
    q.setMaxResults ( 1 );
    Node rt = null;
    try {
      rt = (Node) q.getSingleResult ();
    } catch ( NoResultException ex ) {
      if ( create ) {
        synchronized ( lock ) {
          try {
            em.getTransaction ().begin ();
            Createlocks dblock = Createlocks.getLockObject ( em, "node" );
            em.lock ( dblock, LockModeType.PESSIMISTIC_WRITE );
            rt = findByName ( name, em, false );
            if ( rt == null ) {
              rt = new Node ();
              rt.setName ( name.isEmpty () ? "master" : name );
              em.persist ( rt );
            }
            em.getTransaction ().commit ();
          } catch ( Exception ex2 ) {
            em.getTransaction ().rollback ();
            throw ex2;
          }
        }
      }
    }

    return rt;
  }

}
