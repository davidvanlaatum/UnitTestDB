package org.jenkinsci.plugins.unittestdb.db;

import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.common.base.Strings;
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
@Table(name = "unit_tests")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UnitTest.findAll", query
            = "SELECT u FROM UnitTest u"),
    @NamedQuery(name = "UnitTest.findByUnitTestId", query
            = "SELECT u FROM UnitTest u WHERE u.unitTestId = :unitTestId"),
    @NamedQuery(name = "UnitTest.findByJob", query
            = "SELECT u FROM UnitTest u WHERE u.job.jobId = :job"),
    @NamedQuery(name = "UnitTest.findByJobAndName", query
            = "SELECT u FROM UnitTest u WHERE u.job.jobId = :job AND u.name = :name"),
    @NamedQuery(name = "UnitTest.findUnreliableForJob", query
            = "SELECT u FROM UnitTest u WHERE u.job.jobId = :job AND failure_rate >= :rate AND runs >= :runs")
})
public class UnitTest extends DBObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Object lock = new Object();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "unit_test_id")
    private Integer unitTestId;
    @Basic(optional = false)
    @Column(name = "name")
    private String name;
    @Column(name = "id")
    protected String id;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "failure_rate")
    private Double failureRate;
    @Column(name = "runs")
    private Integer runs;
    @Column(name = "statechanges")
    private Integer statechanges;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "unitTest")
    private List<Failure> failureList;
    @JoinColumn(name = "job_id", referencedColumnName = "job_id")
    @ManyToOne(optional = false)
    private Job job;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "unitTest")
    private List<BuildUnitTest> buildUnitTestList;

    public UnitTest() {
    }

    public UnitTest(Integer unitTestId) {
        this.unitTestId = unitTestId;
    }

    public UnitTest(Integer unitTestId, String name) {
        this.unitTestId = unitTestId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUnitTestId() {
        return unitTestId;
    }

    public void setUnitTestId(Integer unitTestId) {
        this.unitTestId = unitTestId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(Double failureRate) {
        this.failureRate = failureRate;
    }

    @XmlTransient
    public List<Failure> getFailureList() {
        return failureList;
    }

    public void setFailureList(List<Failure> failureList) {
        this.failureList = failureList;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Integer getRuns() {
        return runs;
    }

    public Integer getStatechanges() {
        return statechanges;
    }

    public void setRuns(Integer runs) {
        this.runs = runs;
    }

    public void setStatechanges(Integer statechanges) {
        this.statechanges = statechanges;
    }

    @XmlTransient
    public List<BuildUnitTest> getBuildUnitTestList() {
        return buildUnitTestList;
    }

    public void setBuildUnitTestList(List<BuildUnitTest> buildUnitTestList) {
        this.buildUnitTestList = buildUnitTestList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (unitTestId != null ? unitTestId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UnitTest)) {
            return false;
        }
        UnitTest other = (UnitTest) object;
        if ((this.unitTestId == null && other.unitTestId != null)
                || (this.unitTestId != null && !this.unitTestId.equals(
                        other.unitTestId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.vanlaatum.unittestsdb.DB.UnitTest[ unitTestId=" + unitTestId
                + " ]";
    }

    public static UnitTest findByJobAndName(Job job, String name,
            EntityManager em,
            boolean create) {
        requireNonNull(em, "No EntityManager passed in");
        requireNonNull(job, "No Job passwd in");
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Name is null or empty");
        }
        Query q = em.createNamedQuery("UnitTest.findByJobAndName");
        q.setParameter("job", job.getJobId());
        q.setParameter("name", name);
        q.setMaxResults(1);
        UnitTest rt = null;
        try {
            rt = (UnitTest) q.getSingleResult();
        } catch (NoResultException ex) {
            if (create) {
                synchronized (lock) {
                    try {
                        em.getTransaction().begin();
                        em.lock(job, LockModeType.PESSIMISTIC_WRITE);
                        rt = findByJobAndName(job, name, em, false);
                        if (rt == null) {
                            rt = new UnitTest();
                            rt.setJob(job);
                            rt.setName(name);
                            em.persist(rt);
                        }
                        em.getTransaction().commit();
                    } catch (Exception ex2) {
                        em.getTransaction().rollback();
                        throw ex2;
                    }
                }
            }
        }

        return rt;
    }

    public static SortedMap<String, UnitTest> findByJob(Job job,
            EntityManager em) {
        requireNonNull(em, "No EntityManager passed in");
        requireNonNull(job, "No job passwd in");
        Query q = em.createNamedQuery("UnitTest.findByJob");
        q.setParameter("job", job.getJobId());
        SortedMap<String, UnitTest> rt = new TreeMap<>();
        try {
            for (UnitTest u : (List<UnitTest>) q.getResultList()) {
                rt.put(u.getName(), u);
            }
        } catch (NoResultException ex) {
        }

        return rt;
    }

    public static List<UnitTest> findUnreliableForJob(Job job, EntityManager em) {
        List<UnitTest> rt = null;
        requireNonNull(em, "No EntityManager passed in");
        requireNonNull(job, "No job passwd in");
        Query q = em.createNamedQuery("UnitTest.findUnreliableForJob");
        q.setParameter("job", job.getJobId());
        q.setParameter("rate", 20);
        q.setParameter("runs", 10);
        try {
            rt = q.getResultList();
        } catch (NoResultException ex) {
        }
        return rt;
    }

}
