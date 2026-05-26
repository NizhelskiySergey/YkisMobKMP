Привет, давай поступим так. Чтобы мы работали как два разработчика работающими над одним проектом YkisMobPAM.
Мы должны составить краткую инструкцию которая будет содержать логику проекта и инструменты которыми мы используем.
Я пришлю свою часть, а ты доработаещь и выдаш полностью

Я хочу тебе напомнить  стартовые экраны для приложения. Общее для жильца и админа.
Если он не авторизован, он переходит на экран подтверждения TermsAndConditionScreen.
После подтверждения на экран входа SignInScreen.
Если у него нет аккаунта он переходит на экран SignUpScreen.
Если аккаунт есть, то с помощью ApartmentNavGraph он переходит
для добавления квартиры или ввода секретного кода для админа на экран AddApartmentScreen.
Если у жильца есть квартира, а админ зарегистрирован, то жилец попадает на экран InfoApartmentScreen,
а админ на экран списка выбора чатов квартир UserListScreen.
This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).
Clean Architecture + MVI/MVVM в KMP
1.Repository: Чистая «труба» в интернет. Только Ktor, никаких обращений к SQLDelight.
2.QLDelight:Лежит в commonMain, генерирует методы, но вызывается только через лямбды.
3.UseCase: Единственное место, где принимается решение: «Взять из сети или отдать кэш».
4.Koin: «Клей», который подставляет в UseCase конкретные вызовы к базе.

После удалени в SQLDelight
в терминале
./gradlew clean
./gradlew generateSqlDelightInterface

после добавления ресурсов
./gradlew generateComposeResClass

Счетчики

Entity
data class HeatMeterEntity
data class WaterMeterEntity
data class HeatReadingEntity
data class WaterReadingEntity

model
class MeterScreenModel
WaterMeterList
HeatMeterList
WaterMeterItem
HeatMeterItem

screen


displayName "Користувач"
email "+380938468141"
lastLogin 1779659408
osbbId 0
phoneNumber "+380938468141"
uid   "lFBtv49ou1TpgCyT45BHUNBS0zU2"
userRole "STANDARD_USER"
