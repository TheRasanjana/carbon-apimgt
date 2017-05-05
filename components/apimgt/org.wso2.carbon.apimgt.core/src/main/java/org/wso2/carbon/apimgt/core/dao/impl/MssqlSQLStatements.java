/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.core.dao.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.carbon.apimgt.core.exception.APIMgtDAOException;
import org.wso2.carbon.apimgt.core.models.API;
import org.wso2.carbon.apimgt.core.models.APIStatus;
import org.wso2.carbon.apimgt.core.util.APIMgtConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL Statements that are specific to Ms SQL Database.
 */
public class MssqlSQLStatements implements ApiDAOVendorSpecificStatements {

    private static final String API_SUMMARY_SELECT =
            "SELECT DISTINCT API.UUID, API.PROVIDER, API.NAME, API.CONTEXT, API.VERSION, API.DESCRIPTION,"
                    + "API.CURRENT_LC_STATUS, API.LIFECYCLE_INSTANCE_ID, API.LC_WORKFLOW_STATUS "
                    + "FROM AM_API API LEFT JOIN AM_API_GROUP_PERMISSION PERMISSION ON UUID = API_ID";

    private static final String API_SUMMARY_SELECT_STORE = "SELECT UUID, PROVIDER, NAME, CONTEXT, " +
            "VERSION, DESCRIPTION, CURRENT_LC_STATUS, LIFECYCLE_INSTANCE_ID, LC_WORKFLOW_STATUS " +
            "FROM AM_API ";

    /**
     * Creates full text search query specific to database.
     *
     * @param connection  Database connection.
     * @param searchString The search string provided
     * @param offset  The starting point of the search results.
     * @param limit   Number of search results that will be returned.
     * @return {@link   PreparedStatement} Statement build for specific database type.
     * @throws APIMgtDAOException if error occurs while accessing data layer
     */
    @Override
    @SuppressFBWarnings ({ "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
            "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE" })
    public PreparedStatement search(Connection connection, List<String> roles, String user, String searchString, int
            offset, int
            limit) throws
            APIMgtDAOException {
        StringBuilder roleListBuilder = new StringBuilder();
        roles.forEach(item -> roleListBuilder.append("?,"));
        roleListBuilder.append("?");
        final String query =
                API_SUMMARY_SELECT + " WHERE  CONTAINS(API.*, ?) AND ((GROUP_ID IN (" + roleListBuilder.toString()
                        + ")) OR (PROVIDER = ?))  ORDER BY NAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        int queryIndex = 1;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(queryIndex, "\"" + searchString.toLowerCase(Locale.ENGLISH) + "*\"");
            queryIndex++;
            for (String role : roles) {
                statement.setString(queryIndex, role);
                queryIndex++;
            }
            statement.setString(queryIndex, EVERYONE_ROLE);
            statement.setString(++queryIndex, user);
            statement.setInt(++queryIndex, offset);
            statement.setInt(++queryIndex, limit);
            return statement;
        } catch (SQLException e) {
            throw new APIMgtDAOException(e);
        }
    }

    /**
     * Creates attribute search query specific to database.
     *
     * @param connection  Database connection.
     * @param attributeMap Map containing the attributes and search queries for those attributes
     * @param offset  The starting point of the search results.
     * @param limit   Number of search results that will be returned.
     * @return {@link   PreparedStatement} Statement build for specific database type.
     * @throws APIMgtDAOException if error occurs while accessing data layer
     */
    @Override
    @SuppressFBWarnings ({ "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
            "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE" })
    public PreparedStatement attributeSearch(Connection connection, List<String> roles, String user, Map<String, String>
            attributeMap, int offset,
            int limit) throws APIMgtDAOException {
        StringBuilder roleListBuilder = new StringBuilder();
        roles.forEach(item -> roleListBuilder.append("?,"));
        roleListBuilder.append("?");
        StringBuffer searchQuery = new StringBuffer();
        Iterator<Map.Entry<String, String>> entries = attributeMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            searchQuery.append("LOWER(");
            searchQuery.append(entry.getKey());
            searchQuery.append(") LIKE ?");
            if (entries.hasNext()) {
                searchQuery.append(" AND ");
            }
        }

        final String query =
                API_SUMMARY_SELECT + " WHERE " + searchQuery.toString() + " AND ((GROUP_ID IN (" + roleListBuilder
                        .toString() + ")) OR  (PROVIDER = ?)) ORDER BY NAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try {
            int queryIndex = 1;
            PreparedStatement statement = connection.prepareStatement(query);
            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                statement.setString(queryIndex, '%' + entry.getValue().toLowerCase(Locale.ENGLISH) + '%');
                queryIndex++;
            }
            for (String role : roles) {
                statement.setString(queryIndex, role);
                queryIndex++;
            }
            statement.setString(queryIndex, EVERYONE_ROLE);
            statement.setString(++queryIndex, user);
            statement.setInt(++queryIndex, --offset);
            statement.setInt(++queryIndex, limit);
            return statement;
        } catch (SQLException e) {
            throw new APIMgtDAOException(e);
        }
    }

    /**
     * @see ApiDAOVendorSpecificStatements#attributeSearchStore(Connection connection, List,
     * Map, int, int)
     */
    @Override
    @SuppressFBWarnings({"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
            "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"})
    public PreparedStatement attributeSearchStore(Connection connection, List<String> roles,
                                                  Map<String, String> attributeMap, int offset,
                                                  int limit) throws APIMgtDAOException {

        StringBuilder roleListBuilder = new StringBuilder();
        roleListBuilder.append("?");
        for (int i = 0; i < roles.size() - 1; i++) {
            roleListBuilder.append(",?");
        }
        StringBuilder searchQuery = new StringBuilder();
        Iterator<Map.Entry<String, String>> entries = attributeMap.entrySet().iterator();

        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            searchQuery.append("LOWER(");
            if (APIMgtConstants.TAG_SEARCH_TYPE_PREFIX.equalsIgnoreCase(entry.getKey())) {
                searchQuery.append(APIMgtConstants.TAG_NAME_COLUMN);
            } else if (APIMgtConstants.SUBCONTEXT_SEARCH_TYPE_PREFIX.
                    equalsIgnoreCase(entry.getKey())) {
                searchQuery.append(APIMgtConstants.URL_PATTERN_COLUMN);
            } else {
                searchQuery.append(entry.getKey());
            }
            searchQuery.append(") LIKE ?");
            if (entries.hasNext()) {
                searchQuery.append(" AND ");
            }
        }

        String query = null;
        if (attributeMap.containsKey(APIMgtConstants.TAG_SEARCH_TYPE_PREFIX)) {
            //for tag search, need to check AM_API_TAG_MAPPING and AM_TAGS tables
            query = API_SUMMARY_SELECT_STORE + " WHERE CURRENT_LC_STATUS  IN ('" +
                    APIStatus.PUBLISHED.getStatus() + "','" +
                    APIStatus.PROTOTYPED.getStatus() + "') AND " +
                    "VISIBILITY = '" + API.Visibility.PUBLIC + "' AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_TAG_MAPPING WHERE TAG_ID IN " +
                    "(SELECT TAG_ID FROM AM_TAGS WHERE " + searchQuery.toString() + ")) " +
                    "UNION " +
                    API_SUMMARY_SELECT_STORE + " WHERE CURRENT_LC_STATUS  IN ('" +
                    APIStatus.PUBLISHED.getStatus() + "','" +
                    APIStatus.PROTOTYPED.getStatus() + "') AND " +
                    "VISIBILITY = '" + API.Visibility.RESTRICTED + "' AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_VISIBLE_ROLES WHERE ROLE IN (" +
                    roleListBuilder.toString() + ")) AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_TAG_MAPPING WHERE TAG_ID IN " +
                    "(SELECT TAG_ID FROM AM_TAGS WHERE " + searchQuery.toString() + ")) " +
                    "ORDER BY NAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        } else if (attributeMap.containsKey(APIMgtConstants.SUBCONTEXT_SEARCH_TYPE_PREFIX)) {
            //for subcontext search, need to check AM_API_OPERATION_MAPPING table
            query = API_SUMMARY_SELECT_STORE + " WHERE CURRENT_LC_STATUS  IN ('" +
                    APIStatus.PUBLISHED.getStatus() + "','" +
                    APIStatus.PROTOTYPED.getStatus() + "') AND " +
                    "VISIBILITY = '" + API.Visibility.PUBLIC + "' AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_OPERATION_MAPPING WHERE " +
                    searchQuery.toString() + ") " +
                    "UNION " +
                    API_SUMMARY_SELECT_STORE + " WHERE CURRENT_LC_STATUS  IN ('" +
                    APIStatus.PUBLISHED.getStatus() + "','" +
                    APIStatus.PROTOTYPED.getStatus() + "') AND " +
                    "VISIBILITY = '" + API.Visibility.RESTRICTED + "' AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_VISIBLE_ROLES WHERE ROLE IN (" +
                    roleListBuilder.toString() + ")) AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_OPERATION_MAPPING WHERE " +
                    searchQuery.toString() + ") " +
                    "ORDER BY NAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        } else {
            //for any other attribute search, need to check AM_API table
            query = API_SUMMARY_SELECT_STORE + " WHERE CURRENT_LC_STATUS  IN ('" +
                    APIStatus.PUBLISHED.getStatus() + "','" +
                    APIStatus.PROTOTYPED.getStatus() + "') AND " +
                    "VISIBILITY = '" + API.Visibility.PUBLIC + "' AND " +
                    searchQuery.toString() +
                    " UNION " +
                    API_SUMMARY_SELECT_STORE + " WHERE CURRENT_LC_STATUS  IN ('" +
                    APIStatus.PUBLISHED.getStatus() + "','" +
                    APIStatus.PROTOTYPED.getStatus() + "') AND " +
                    "VISIBILITY = '" + API.Visibility.RESTRICTED + "' AND " +
                    "UUID IN (SELECT API_ID FROM AM_API_VISIBLE_ROLES WHERE ROLE IN (" +
                    roleListBuilder.toString() + ")) AND " +
                    searchQuery.toString() +
                    " ORDER BY NAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        }
        try {
            int queryIndex = 1;
            PreparedStatement statement = connection.prepareStatement(query);
            //include the attribute in the query (for APIs with public visibility)
            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                statement.setString(queryIndex, '%' + entry.getValue().
                        toLowerCase(Locale.ENGLISH) + '%');
                queryIndex++;
            }
            //include user roles in the query
            for (String role : roles) {
                statement.setString(queryIndex, role);
                queryIndex++;
            }
            //include the attribute in the query (for APIs with restricted visibility)
            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                statement.setString(queryIndex, '%' + entry.getValue().
                        toLowerCase(Locale.ENGLISH) + '%');
                queryIndex++;
            }
            //setting 0 as the default offset based on store-api.yaml and MSSQL specifications
            statement.setInt(queryIndex, (offset < 0) ? 0 : offset);
            statement.setInt(++queryIndex, limit);
            return statement;
        } catch (SQLException e) {
            throw new APIMgtDAOException(e);
        }
    }
}
