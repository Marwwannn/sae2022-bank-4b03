package model.orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import model.data.Client;
import model.data.Employe;
import model.orm.exception.DataAccessException;
import model.orm.exception.DatabaseConnexionException;
import model.orm.exception.Order;
import model.orm.exception.RowNotFoundOrTooManyRowsException;
import model.orm.exception.Table;

public class AccessEmploye {

	public AccessEmploye() {
	}


	/**
	 * @param idAg
	 * @param idEmploye
	 * @param debutNom
	 * @param debutPrenom
	 * @return
	 * @throws DataAccessException
	 * @throws DatabaseConnexionException
	 */
	public ArrayList<Employe> getEmployes(int idAg, int idEmploye, String login, String mdp)
			throws DataAccessException, DatabaseConnexionException {
		ArrayList<Employe> alResult = new ArrayList<>();

		try {
			Connection con = LogToDatabase.getConnexion();

			PreparedStatement pst;

			String query;
			if (idEmploye != -1) {
				query = "SELECT * FROM Employe where idAg = ?";
				query += " AND idEmploye = ?";
				query += " ORDER BY login";
				pst = con.prepareStatement(query);
				pst.setInt(1, idAg);
				pst.setInt(2, idEmploye);

			} else if (!login.equals("")) {
				login = login.toUpperCase() + "%";
				login = login.toUpperCase() + "%";
				query = "SELECT * FROM Employe where idAg = ?";
				query += " AND UPPER(login) like ?" + " AND UPPER(motpasse) like ?";
				query += " ORDER BY login";
				pst = con.prepareStatement(query);
				pst.setInt(1, idAg);
				pst.setString(2, login);
				pst.setString(3, mdp);
			} else {
				query = "SELECT * FROM Employe where idAg = ?";
				query += " ORDER BY login";
				pst = con.prepareStatement(query);
				pst.setInt(1, idAg);
			}
			System.err.println(query + " login : " + login + " mdp : " + mdp + "#");

			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				int idEmp = rs.getInt("idEmploye");
				String nom = rs.getString("nom");
				String prenom = rs.getString("prenom");
				String droitAccess = rs.getString("droitsaccess");
				droitAccess = (droitAccess == null ? "" : droitAccess);
				String identifiant = rs.getString("login");
				identifiant = (identifiant == null ? "" : identifiant);
				String password = rs.getString("motpasse");
				password = (password == null ? "" : password);
				int idAgEmp = rs.getInt("idAg");

				alResult.add(
						new Employe(idEmp, nom, prenom, droitAccess, identifiant, password, idAgEmp));
			}
			rs.close();
			pst.close();
		} catch (SQLException e) {
			throw new DataAccessException(Table.Client, Order.SELECT, "Erreur accès", e);
		}

		return alResult;
	}


	/**
	 * Recherche d'un employe par son login / mot de passe.
	 *
	 * @param login    login de l'employé recherché
	 * @param password mot de passe donné
	 * @return un Employe ou null si non trouvé
	 * @throws RowNotFoundOrTooManyRowsException
	 * @throws DataAccessException
	 * @throws DatabaseConnexionException
	 */
	public Employe getEmploye(String login, String password)
			throws RowNotFoundOrTooManyRowsException, DataAccessException, DatabaseConnexionException {
		Employe employeTrouve;

		try {
			Connection con = LogToDatabase.getConnexion();
			String query = "SELECT * FROM Employe WHERE" + " login = ?" + " AND motPasse = ?";

			PreparedStatement pst = con.prepareStatement(query);
			pst.setString(1, login);
			pst.setString(2, password);

			ResultSet rs = pst.executeQuery();

			System.err.println(query);

			if (rs.next()) {
				int idEmployeTrouve = rs.getInt("idEmploye");
				String nom = rs.getString("nom");
				String prenom = rs.getString("prenom");
				String droitsAccess = rs.getString("droitsAccess");
				String loginTROUVE = rs.getString("login");
				String motPasseTROUVE = rs.getString("motPasse");
				int idAgEmploye = rs.getInt("idAg");

				employeTrouve = new Employe(idEmployeTrouve, nom, prenom, droitsAccess, loginTROUVE, motPasseTROUVE,
						idAgEmploye);
			} else {
				rs.close();
				pst.close();
				// Non trouvé
				return null;
			}

			if (rs.next()) {
				// Trouvé plus de 1 ... bizarre ...
				rs.close();
				pst.close();
				throw new RowNotFoundOrTooManyRowsException(Table.Employe, Order.SELECT,
						"Recherche anormale (en trouve au moins 2)", null, 2);
			}
			rs.close();
			pst.close();
			return employeTrouve;
		} catch (SQLException e) {
			throw new DataAccessException(Table.Employe, Order.SELECT, "Erreur accès", e);
		}
	}

	/**
	 * Insertion d'un employé.
	 *
	 * @param employé IN/OUT Tous les attributs IN sauf idEmploye en OUT
	 * @throws RowNotFoundOrTooManyRowsException
	 * @throws DataAccessException
	 * @throws DatabaseConnexionException
	 */
	public void insertEmploye(Employe employe)
			throws RowNotFoundOrTooManyRowsException, DataAccessException, DatabaseConnexionException {
		try {

			Connection con = LogToDatabase.getConnexion();

			String query = "INSERT INTO EMPLOYE VALUES (" + "seq_id_employe.NEXTVAL" + ", " + "?" + ", " + "?" + ", "
					+ "?" + ", " + "?" + ", " + "?" + ", " + "?" + ")";
			PreparedStatement pst = con.prepareStatement(query);
			pst.setString(1, employe.nom);
			pst.setString(2, employe.prenom);
			pst.setString(3, employe.droitsAccess);
			pst.setString(4, employe.login);
			pst.setString(5, employe.motPasse);
			pst.setInt(6, employe.idAg);

			System.err.println(query);

			int result = pst.executeUpdate();
			pst.close();

			if (result != 1) {
				con.rollback();
				throw new RowNotFoundOrTooManyRowsException(Table.Employe, Order.INSERT,
						"Insert anormal (insert de moins ou plus d'une ligne)", null, result);
			}

			query = "SELECT seq_id_employe.CURRVAL from DUAL";

			System.err.println(query);
			PreparedStatement pst2 = con.prepareStatement(query);

			ResultSet rs = pst2.executeQuery();
			rs.next();
			int numCliBase = rs.getInt(1);

			con.commit();
			rs.close();
			pst2.close();

			employe.idEmploye = numCliBase;
		} catch (SQLException e) {
			throw new DataAccessException(Table.Employe, Order.INSERT, "Erreur accès", e);
		}
	}

	/**
	 * Mise à jour d'un Employé.
	 *
	 * employe.idEmploye est la clé primaire et doit exister tous les autres champs
	 * sont des mises à jour. employe.idAg non mis à jour (l'employé ne change
	 * d'agence que par delete/insert)
	 *
	 * @param employe IN employe.idEmploye (clé primaire) doit exister
	 * @throws RowNotFoundOrTooManyRowsException
	 * @throws DataAccessException
	 * @throws DatabaseConnexionException
	 */
	public void updateEmploye(Employe employe)
			throws RowNotFoundOrTooManyRowsException, DataAccessException, DatabaseConnexionException {
		try {
			Connection con = LogToDatabase.getConnexion();

			String query = "UPDATE EMPLOYE SET " + "nom = " + "? , " + "prenom = " + "? , " + "droitsaccess = "
					+ "? , " + "login = " + "? , " + "motpasse = " + "?"  + " "
					+ "WHERE idemploye = ? ";

			PreparedStatement pst = con.prepareStatement(query);
			pst.setString(1, employe.nom);
			pst.setString(2, employe.prenom);
			pst.setString(3, employe.droitsAccess);
			pst.setString(4, employe.login);
			pst.setString(5, employe.motPasse);
			pst.setInt(6, employe.idEmploye);

			System.err.println(query);

			int result = pst.executeUpdate();
			pst.close();
			if (result != 1) {
				con.rollback();
				throw new RowNotFoundOrTooManyRowsException(Table.Employe, Order.UPDATE,
						"Update anormal (update de moins ou plus d'une ligne)", null, result);
			}
			con.commit();
		} catch (SQLException e) {
			throw new DataAccessException(Table.Employe, Order.UPDATE, "Erreur accès", e);
		}
	}



	public void desacEmploye(Employe employe)
			throws RowNotFoundOrTooManyRowsException, DataAccessException, DatabaseConnexionException {
		try {

			Connection con = LogToDatabase.getConnexion();

			String query = "UPDATE employe SET login = null, motpasse = null WHERE idemploye = ?";
			PreparedStatement pst = con.prepareStatement(query);
			pst.setInt(1, employe.idEmploye);


			System.err.println(query);
			System.out.println(employe.idEmploye);
			int result = pst.executeUpdate();
			pst.close();

			if (result != 1) {
				con.rollback();
				throw new RowNotFoundOrTooManyRowsException(Table.Employe, Order.DELETE,
						"Delete anormal (delete de moins ou plus d'une ligne)", null, result);
			}

			query = "SELECT seq_id_employe.CURRVAL from DUAL";

			System.err.println(query);
			PreparedStatement pst2 = con.prepareStatement(query);

			con.commit();

		} catch (SQLException e) {
			throw new DataAccessException(Table.Employe, Order.INSERT, "Erreur accès", e);
		}
	}
	
	public void reacEmploye(Employe employe)
			throws RowNotFoundOrTooManyRowsException, DataAccessException, DatabaseConnexionException {
		try {

			Connection con = LogToDatabase.getConnexion();

			String query = "UPDATE employe SET login = ?, motpasse = ? WHERE idemploye = ?";
			PreparedStatement pst = con.prepareStatement(query);
			pst.setString(1, employe.login);
			pst.setString(2, employe.motPasse);
			pst.setInt(3, employe.idEmploye);


			System.err.println(query);
			int result = pst.executeUpdate();
			pst.close();

			if (result != 1) {
				con.rollback();
				throw new RowNotFoundOrTooManyRowsException(Table.Employe, Order.DELETE,
						"Delete anormal (delete de moins ou plus d'une ligne)", null, result);
			}
			con.commit();
			
		} catch (SQLException e) {
			throw new DataAccessException(Table.Employe, Order.INSERT, "Erreur accès", e);
		}
	}
}
