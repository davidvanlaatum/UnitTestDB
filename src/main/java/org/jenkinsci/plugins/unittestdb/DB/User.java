package org.jenkinsci.plugins.unittestdb.DB;

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
@Table ( name = "users" )
@XmlRootElement
@NamedQueries ( {
  @NamedQuery ( name = "User.findAll", query
                = "SELECT u FROM User u" ),
  @NamedQuery ( name = "User.findByUserId", query
                = "SELECT u FROM User u WHERE u.userId = :userId" ),
  @NamedQuery ( name = "User.findByUsername", query
                = "SELECT u FROM User u WHERE u.username = :username" ) } )
public class User extends DBObject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue ( strategy = GenerationType.IDENTITY )
  @Basic ( optional = false )
  @Column ( name = "user_id" )
  private Integer userId;
  @Basic ( optional = false )
  @Column ( name = "username" )
  private String username;
  @OneToMany ( cascade = CascadeType.ALL, mappedBy = "user" )
  private List<FailureUser> failureUserList;

  public User () {
  }

  public User ( Integer userId ) {
    this.userId = userId;
  }

  public User ( Integer userId, String username ) {
    this.userId = userId;
    this.username = username;
  }

  public Integer getUserId () {
    return userId;
  }

  public void setUserId ( Integer userId ) {
    this.userId = userId;
  }

  public String getUsername () {
    return username;
  }

  public void setUsername ( String username ) {
    this.username = username;
  }

  @XmlTransient
  public List<FailureUser> getFailureUserList () {
    return failureUserList;
  }

  public void setFailureUserList ( List<FailureUser> failureUserList ) {
    this.failureUserList = failureUserList;
  }

  @Override
  public int hashCode () {
    int hash = 0;
    hash += ( userId != null ? userId.hashCode () : 0 );
    return hash;
  }

  @Override
  public boolean equals ( Object object ) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if ( !( object instanceof User ) ) {
      return false;
    }
    User other = (User) object;
    if ( ( this.userId == null && other.userId != null ) || ( this.userId
                                                              != null
                                                              && !this.userId
                                                             .equals (
                                                             other.userId ) ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString () {
    return "com.vanlaatum.unittestsdb.DB.User[ userId=" + userId + " ]";
  }

  public static User findByUsername ( String name, EntityManager em,
                                         boolean create ) {
    requireNonNull ( em, "No EntityManager passed in" );
    if ( Strings.isNullOrEmpty ( name ) ) {
      throw new IllegalArgumentException ( "Name is null or empty" );
    }
    Query q = em.createNamedQuery ( "User.findByUsername" );
    q.setParameter ( "username", name );
    q.setMaxResults ( 1 );
    User rt = null;
    try {
      rt = (User) q.getSingleResult ();
    } catch ( NoResultException ex ) {
      if ( create ) {
        rt = new User ();
        rt.setUsername ( name );
        try {
          em.persist ( rt );
        } catch ( EntityExistsException ex2 ) {
          rt = findByUsername ( name, em, false );
        }
      }
    }

    return rt;
  }

}
