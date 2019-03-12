package io.mosip.authentication.service.impl.indauth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.dto.indauth.AuthRequestDTO;
import io.mosip.authentication.core.dto.indauth.AuthResponseDTO;
import io.mosip.authentication.core.dto.indauth.KycAuthRequestDTO;
import io.mosip.authentication.core.dto.indauth.KycAuthResponseDTO;
import io.mosip.authentication.core.exception.IDDataValidationException;
import io.mosip.authentication.core.exception.IdAuthenticationAppException;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.exception.IdAuthenticationDaoException;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.spi.indauth.facade.AuthFacade;
import io.mosip.authentication.core.util.DataValidationUtil;
import io.mosip.authentication.service.impl.indauth.validator.AuthRequestValidator;
import io.mosip.authentication.service.impl.indauth.validator.KycAuthRequestValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

/**
 * The {@code AuthController} used to handle all the authentication requests.
 *
 * @author Arun Bose
 * @author Prem Kumar
 */
@RestController
public class AuthController {

	/** The Constant SESSION_ID. */
	private static final String SESSION_ID = "sessionId";

	/** The mosipLogger. */
	private Logger mosipLogger = IdaLogger.getLogger(AuthController.class);

	/** The auth request validator. */
	@Autowired
	private AuthRequestValidator authRequestValidator;

	/** The KycAuthRequestValidator */
	@Autowired
	private KycAuthRequestValidator kycReqValidator;

	/** The auth facade. */
	@Autowired
	private AuthFacade authFacade;

	/**
	 *
	 * @param binder the binder
	 */
	@InitBinder("authRequestDTO")
	private void initAuthRequestBinder(WebDataBinder binder) {
		binder.setValidator(authRequestValidator);
	}

	/**
	 *
	 * @param binder the binder
	 */
	@InitBinder("kycAuthRequestDTO")
	private void initKycBinder(WebDataBinder binder) {
		binder.addValidators(kycReqValidator);
	}

	/**
	 * authenticateRequest - method to authenticate request.
	 *
	 * @param authrequestdto - Authenticate Request
	 * @param errors         the errors
	 * @return authResponsedto AuthResponseDTO
	 * @throws IdAuthenticationAppException the id authentication app exception
	 * @throws IdAuthenticationDaoException the id authentication dao exception
	 */
	@PostMapping(path = "/auth/0.8/{Auth-Partner-ID}/{MISP-LK}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Authenticate Request", response = IdAuthenticationAppException.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully"),
			@ApiResponse(code = 400, message = "Request authenticated failed") })
	public AuthResponseDTO authenticateApplication(@Validated @RequestBody AuthRequestDTO authrequestdto,
			@ApiIgnore Errors errors, @PathVariable("Auth-Partner-ID") String partnerId,
			@PathVariable("MISP-LK") String mispLK) throws IdAuthenticationAppException, IdAuthenticationDaoException {
		AuthResponseDTO authResponsedto = null;

		try {
			DataValidationUtil.validate(errors);

			authResponsedto = authFacade.authenticateApplicant(authrequestdto, true,partnerId,mispLK);
		} catch (IDDataValidationException e) {
			mosipLogger.error(SESSION_ID, this.getClass().getSimpleName(), "authenticateApplication",
					e.getErrorTexts().isEmpty() ? "" : e.getErrorText());
			throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.DATA_VALIDATION_FAILED, e);
		} catch (IdAuthenticationBusinessException e) {
			mosipLogger.error(SESSION_ID, this.getClass().getSimpleName(), "authenticateApplication",
					e.getErrorTexts().isEmpty() ? "" : e.getErrorText());
			throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.AUTHENTICATION_FAILED, e);
		}

		return authResponsedto;
	}

	/**
	 * Controller Method to auhtentication for eKyc-Details.
	 *
	 * @param kycAuthRequestDTO the kyc auth request DTO
	 * @param errors            the errors
	 * @return kycAuthResponseDTO the kyc auth response DTO
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 * @throws IdAuthenticationAppException      the id authentication app exception
	 * @throws IdAuthenticationDaoException      the id authentication dao exception
	 */
	@PostMapping(path = "/kyc/0.8/{eKYC-Partner-ID}/{MISP-LK}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "eKyc Request", response = IdAuthenticationAppException.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully"),
			@ApiResponse(code = 400, message = "Request authenticated failed") })
	public KycAuthResponseDTO processKyc(@Validated @RequestBody KycAuthRequestDTO kycAuthRequestDTO,
			@ApiIgnore Errors errors, @PathVariable("eKYC-Partner-ID") String partnerId,
			@PathVariable("MISP-LK") String mispLK)
			throws IdAuthenticationBusinessException, IdAuthenticationAppException, IdAuthenticationDaoException {
		AuthResponseDTO authResponseDTO = null;
		KycAuthResponseDTO kycAuthResponseDTO = new KycAuthResponseDTO();
		try {
			DataValidationUtil.validate(errors);
			authResponseDTO = authFacade.authenticateApplicant(kycAuthRequestDTO, true,partnerId,mispLK);
			if (authResponseDTO != null) {
				kycAuthResponseDTO = authFacade.processKycAuth(kycAuthRequestDTO, authResponseDTO,partnerId);
			}
		} catch (IDDataValidationException e) {
			mosipLogger.error(SESSION_ID, this.getClass().getSimpleName(), "processKyc",
					e.getErrorTexts().isEmpty() ? "" : e.getErrorText());
			throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.DATA_VALIDATION_FAILED, e);
		} catch (IdAuthenticationBusinessException e) {
			mosipLogger.error(SESSION_ID, this.getClass().getSimpleName(), "processKyc",
					e.getErrorTexts().isEmpty() ? "" : e.getErrorText());
			throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.AUTHENTICATION_FAILED, e);
		}
		return kycAuthResponseDTO;
	}

}
