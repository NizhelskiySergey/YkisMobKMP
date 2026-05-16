package com.ykis.ykismobkmp.ui.screens.appartment

import com.ykis.ykismobkmp.domain.repository.apartment.useCase.AddApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.DeleteApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.DeleteUserAccount
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetApartmentList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetHouseList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetOsbbApartmentsList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetRaionList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.SaveUserUid
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.UpdateBti
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.VerifyAdminCode


class ApartmentService(
  val getApartmentList: GetApartmentList,     // Для жильца (с БД)
  val getOsbbApartmentsList: GetOsbbApartmentsList, // Для админа (чистая сеть)
  val getRaionList: GetRaionList, // Для админа (чистая сеть)
  val getHouseList: GetHouseList, // Для админа (чистая сеть)
  val getApartment: GetApartment,
  val addApartment: AddApartment,
  val verifyAdminCode: VerifyAdminCode,
  val deleteApartment: DeleteApartment,
  val updateBti: UpdateBti,
  val saveUserUid: SaveUserUid,
  val deleteUserAccount: DeleteUserAccount
)



