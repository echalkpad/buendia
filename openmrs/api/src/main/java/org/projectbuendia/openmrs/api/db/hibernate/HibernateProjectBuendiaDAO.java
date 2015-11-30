// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.openmrs.api.db.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.projectbuendia.openmrs.PatientSyncParameters;
import org.projectbuendia.openmrs.api.SyncToken;
import org.projectbuendia.openmrs.api.db.ProjectBuendiaDAO;
import org.projectbuendia.openmrs.api.db.SyncPage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hibernate.criterion.Order.asc;
import static org.hibernate.criterion.Restrictions.disjunction;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.ge;
import static org.hibernate.criterion.Restrictions.sqlRestriction;

/** Default implementation of {@link ProjectBuendiaDAO}. */
public class HibernateProjectBuendiaDAO implements ProjectBuendiaDAO {
    protected final Log log = LogFactory.getLog(this.getClass());

    private SessionFactory sessionFactory;

    /** @return the sessionFactory */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /** @param sessionFactory the sessionFactory to set */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Obs> getObservationsModifiedAtOrAfter(@Nullable Date date, boolean includeVoided) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Obs.class);
        if (!includeVoided) {
            criteria.add(eq("voided", false));
        }

        if (date != null) {
            Disjunction orClause = disjunction();
            orClause.add(ge("dateCreated", date));

            if (includeVoided) {
                orClause.add(ge("dateVoided", date));
            }
            criteria.add(orClause);
        }
        //noinspection unchecked
        return criteria.list();
    }

    @Override
    public SyncPage<Patient> getPatientsModifiedAfter(
            @Nullable SyncToken syncToken, boolean includeVoided, int maxResults) {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria(PatientSyncParameters.class);

        if (syncToken != null) {
            // (a, b) > (x, y) is equivalent to (a > x) OR ((a = x) AND (b > y)). See
            // http://dev.mysql.com/doc/refman/5.7/en/comparison-operators.html#operator_greater-than
            criteria.add(sqlRestriction(
                    // {alias} is substituted for the table alias that hibernate uses for the type.
                    "({alias}.date_updated, {alias}.uuid) > (?, ?)",
                    new Object[]{
                            syncToken.greaterThanOrEqualToTimestamp,
                            // If syncToken.greaterThanUuid is null, we use the empty string, which
                            // is 'smaller' than every other string in terms of sort order.
                            syncToken.greaterThanUuid == null ? "" : syncToken.greaterThanUuid},
                    new Type[] {StandardBasicTypes.TIMESTAMP, StandardBasicTypes.STRING}));
        }

        if (!includeVoided) {
            criteria.createCriteria("patient").add(eq("personVoided", false));
        }

        criteria.addOrder(asc("dateUpdated"))
                .addOrder(asc("uuid"));

        if (maxResults > 0) {
            criteria.setMaxResults(maxResults);
        }

        //noinspection unchecked
        List<PatientSyncParameters> dbList = criteria.list();

        // PatientSyncParameters --> Patient
        ArrayList<Patient> patients = new ArrayList<>(dbList.size());
        for (PatientSyncParameters params : dbList) {
            patients.add(params.getPatient());
        }

        SyncToken token = null;
        if (dbList.size() > 0) {
            PatientSyncParameters lastEntry = dbList.get(dbList.size() - 1);
            token = new SyncToken(lastEntry.getDateUpdated(), lastEntry.getUuid());
        }
        return new SyncPage<>(patients, token);
    }

    @Override
    public List<Order> getOrdersModifiedAtOrAfter(@Nullable Date date, boolean includeVoided) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Order.class);
        if (!includeVoided) {
            criteria.add(eq("voided", false));
        }
        if (date != null) {
            Disjunction orClause = disjunction();
            orClause.add(ge("dateCreated", date));

            if (includeVoided) {
                orClause.add(ge("dateVoided", date));
            }
            criteria.add(orClause);
        }
        //noinspection unchecked
        return criteria.list();
    }
}