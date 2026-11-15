#!/usr/bin/env python3
"""Reproducible vector UML PDF with Python stdlib; no application/runtime dependencies.
Names are checked against source declarations; diagrams deliberately omit trivial accessors.
Run from the Git root: python3 hotel-management-system/docs/uml/generar_uml.py
"""
from pathlib import Path
import re, math, hashlib
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'hotel-management-system/src/main/java'
OUT=ROOT/'output/pdf/hoteluq-clases.pdf'
W,H=1191,842
class PDF:
 def __init__(self):self.pages=[];self.ops=[]
 def text(self,x,y,s,size=12,bold=False,color=(.10,.18,.27)):
  s=str(s).replace('\\','\\\\').replace('(','\\(').replace(')','\\)').replace('\n',' ')
  self.ops.append(f'{color[0]} {color[1]} {color[2]} rg BT /F{2 if bold else 1} {size} Tf 1 0 0 1 {x:.2f} {H-y:.2f} Tm ({s}) Tj ET')
 def rect(self,x,y,w,h,fill=(1,1,1)):
  self.ops.append(f'{fill[0]} {fill[1]} {fill[2]} rg 0.16 0.27 0.36 RG 0.8 w {x} {H-y-h} {w} {h} re B')
 def line(self,pts,dash=False):
  self.ops.append('0.24 0.32 0.40 RG 1 w '+('[5 3] 0 d ' if dash else '[] 0 d ')+f'{pts[0][0]} {H-pts[0][1]} m '+' '.join(f'{x} {H-y} l' for x,y in pts[1:])+' S [] 0 d')
 def polygon(self,pts,fill=False):
  self.ops.append(('0.18 0.29 0.39 rg ' if fill else '1 1 1 rg ')+f'{pts[0][0]} {H-pts[0][1]} m '+' '.join(f'{x} {H-y} l' for x,y in pts[1:])+' h B')
 def page(self,title,note):
  if self.ops:self.pages.append('\n'.join(self.ops));self.ops=[]
  self.rect(0,0,W,64,(.06,.22,.30));self.text(35,31,'HOTELUQ  /  UML DEL CODIGO REAL',18,True,(1,1,1));self.text(35,51,title,13,False,(.83,.93,.95))
  self.text(35,91,note,11)
 def finish(self):
  if self.ops:self.pages.append('\n'.join(self.ops));self.ops=[]
  objects=[b'',b'',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>'];kids=[]
  for i,content in enumerate(self.pages,1):
   footer=f'0.35 0.42 0.49 rg BT /F1 10 Tf 1 0 0 1 35 22 Tm (H-06 | Arbol local sin commit | Pagina {i} / {len(self.pages)} | 15/11/2026) Tj ET'
   stream=(content+'\n'+footer).encode('cp1252');page_id=len(objects)+1;content_id=page_id+1;kids.append(page_id)
   objects.append(f'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {W} {H}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents {content_id} 0 R >>'.encode())
   objects.append(b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'\nendstream')
  objects[0]=b'<< /Type /Catalog /Pages 2 0 R >>';objects[1]=('<< /Type /Pages /Count '+str(len(kids))+' /Kids ['+' '.join(f'{n} 0 R' for n in kids)+'] >>').encode()
  data=bytearray(b'%PDF-1.4\n%\xe2\xe3\xcf\xd3\n');offset=[0]
  for n,obj in enumerate(objects,1):offset.append(len(data));data.extend(f'{n} 0 obj\n'.encode()+obj+b'\nendobj\n')
  start=len(data);data.extend(f'xref\n0 {len(objects)+1}\n0000000000 65535 f \n'.encode())
  for o in offset[1:]:data.extend(f'{o:010} 00000 n \n'.encode())
  data.extend(f'trailer << /Size {len(objects)+1} /Root 1 0 R >>\nstartxref\n{start}\n%%EOF\n'.encode());OUT.parent.mkdir(parents=True,exist_ok=True);OUT.write_bytes(data)
P=PDF();N={}
def node(name,x,y,w=235,fields=(),methods=(),kind='class',h=135):
 simple=name.split('.')[-1]
 assert any(re.search(r'\b(?:class|interface|record|enum)\s+'+re.escape(simple)+r'\b',p.read_text()) for p in SRC.rglob('*.java')),name
 N[name]=(x,y,w,h);P.rect(x,y,w,h);P.rect(x,y,w,38,(.89,.95,.96));P.text(x+10,y+15,'<<'+kind+'>>',10);P.text(x+10,y+31,name,12,True)
 yy=y+56
 for text in fields:P.text(x+10,yy,text,11);yy+=15
 P.line([(x,y+76),(x+w,y+76)])
 yy=max(yy+4,y+94)
 for text in methods:P.text(x+10,yy,text,11);yy+=15
 return name
def anchor(name,side):
 x,y,w,h=N[name];return {'t':(x+w/2,y),'b':(x+w/2,y+h),'l':(x,y+h/2),'r':(x+w,y+h/2)}[side]
def edge(a,b,label='',kind='assoc',s='r',t='l',via=(),pos=None):
 start,end=anchor(a,s),anchor(b,t);pts=[start,*via,end];P.line(pts,kind in ['realize','dep'])
 dx,dy=end[0]-pts[-2][0],end[1]-pts[-2][1];length=math.hypot(dx,dy) or 1;ux,uy=dx/length,dy/length
 if kind in ['inherit','realize']:
  P.polygon([end,(end[0]-ux*14-uy*7,end[1]-uy*14+ux*7),(end[0]-ux*14+uy*7,end[1]-uy*14-ux*7)])
 if kind=='dep':P.line([(end[0]-ux*10-uy*5,end[1]-uy*10+ux*5),end,(end[0]-ux*10+uy*5,end[1]-uy*10-ux*5)])
 if kind in ['compose','aggregate']:
  dx,dy=pts[1][0]-start[0],pts[1][1]-start[1];l=math.hypot(dx,dy) or 1;u,v=dx/l,dy/l
  P.polygon([start,(start[0]+u*9-v*5,start[1]+v*9+u*5),(start[0]+u*18,start[1]+v*18),(start[0]+u*9+v*5,start[1]+v*9-u*5)],kind=='compose')
 if label:
  x,y=pos or ((start[0]+end[0])/2,(start[1]+end[1])/2-8);P.rect(x-3,y-11,max(35,len(label)*5.3),15,(1,1,1));P.text(x,y,label,10)
def note(y,*lines):
 for i,text in enumerate(lines):P.text(38,y+i*17,text,11)
P.page('1. Dominio: usuarios, catalogos y dinero','Rombo lleno: valor exclusivo; rombo vacio: registro/agrupacion; linea: referencia compartida. Multiplicidades en los extremos/roles.')
node('User',40,140,fields=['- id, name, email','- passwordHash, salt'],methods=['+ checkPassword(password)'],kind='abstract')
node('Client',40,355,fields=['- card: VirtualCard'],methods=['+ getCard()'])
node('Administrator',315,355,fields=['role: ADMINISTRATOR'],methods=[])
node('VirtualCard',40,570,fields=['- balance: BigDecimal','- movements: List'],methods=['+ recharge(...)','+ bookingTransfer(...)'])
node('CardMovement',315,570,fields=['id, amount, type, at','reservationId, paymentId'],methods=['+ getExactAmount()'])
node('Room',640,140,fields=['number, floor, active','type: RoomType'],methods=['+ getFeatures()'],kind='abstract')
node('RoomType',915,140,fields=['category, pricePerNight','capacity, active'],methods=['+ updateDetails(...)'])
node('AdditionalService',640,355,fields=['id, name, price','available'],methods=['+ updateDetails(...)'])
node('Promotion',915,355,fields=['id, name, discountPercent'],methods=['+ updateDetails(...)'])
node('Hotel',640,570,fields=['registered collections','one synchronization monitor'],methods=['+ getInstance()','+ withSession(...)'])
edge('Client','User','',kind='inherit',s='t',t='b');edge('Administrator','User','',kind='inherit',s='t',t='b',via=[(432,320),(157,320)])
edge('Client','VirtualCard','1 card',kind='compose',s='b',t='t',pos=(70,535));edge('VirtualCard','CardMovement','1 -> 0..* movements',kind='compose',pos=(276,557))
edge('Room','RoomType','0..* -> 1 type',pos=(865,180));edge('Hotel','AdditionalService','0..* registered',kind='aggregate',s='t',t='b',pos=(680,530));edge('Hotel','Promotion','0..* registered',kind='aggregate',s='r',t='b',via=[(1030,637)],pos=(928,561))
note(748,'Room has StandardRoom, FamilyRoom and SuiteRoom subtypes (page 4). Client and Administrator inherit User.', 'Hotel registers references; shared catalog entities are not copied into reservations. Financial values are immutable records/snapshots.')
P.page('2. Reserva, contratos y State','Una reserva conserva su identidad y copias historicas. La UI no cambia State directamente: Hotel autoriza y coordina la transaccion.')
node('Reservation',40,140,w=260,fields=['client, terms, state','payment, refund, history'],methods=['+ pay(), cancel(), complete()','+ checkpoint(), restore(...)'])
node('BookingRecords.Quote',390,140,w=260,fields=['prices, dates, service lines','promotionDefinition snapshot'],methods=['+ currency(): COP'],kind='record')
node('ReservationState',820,140,w=300,fields=[],methods=['+ pay(Reservation)','+ cancel(Reservation, policy)','+ complete(Reservation)'],kind='interface')
for name,x,y in [('PendingState',40,400),('ConfirmedState',325,400),('CancelledState',610,400),('CompletedState',895,400)]:node(name,x,y,w=245,fields=['status is explicit'],methods=['transitions or rejection'])
edge('Reservation','BookingRecords.Quote','1 current contract',kind='compose',pos=(305,170));edge('Reservation','ReservationState','1 current state',s='b',t='l',via=[(170,325),(770,325),(770,205)],pos=(420,315))
for name in ['PendingState','ConfirmedState','CancelledState','CompletedState']:edge(name,'ReservationState','',kind='realize',s='t',t='b',via=[(anchor(name,'t')[0],355),(970,355)])
node('BookingRecords.Payment',40,615,w=260,fields=['reservationId, card, amount','at, currency'],methods=[],kind='record')
node('BookingRecords.Refund',390,615,w=300,fields=['paymentId, amount, percent','policy/version, byHotel, at'],methods=[],kind='record')
node('BookingRecords.Checkout',820,615,w=300,fields=['operationId, actorId, at','scheduledAt, authorizedWindow'],methods=[],kind='record')
note(570,'Owned values per Reservation: Payment 0..1, Refund 0..1, Checkout 0..1 and AuditEvent 0..*. Quote contains ServiceLine 0..*.', 'State protocol: PENDING -> CONFIRMED/CANCELLED; CONFIRMED -> CANCELLED/COMPLETED. COMPLETED only through recorded checkout.')
P.page('3. Strategy de cancelacion y reglas comunes','Norma monetaria: cliente >72 h = 100%; [24,72] h = 80%; <24 h = 0%; hotel = 100%. Se leen instantes de Bogota.')
node('RefundPolicy',420,140,w=310,fields=[],methods=['+ calculateRefund(BigDecimal)','+ getName()'],kind='interface')
for name,x in [('FullRefundPolicy',40),('PartialRefundPolicy',325),('NoRefundPolicy',610),('CancellationSchedule.ConfiguredRefundPolicy',895)]:
 node(name,x,380,w=245,fields=['calculation in BigDecimal'],methods=['+ calculateRefund(...)']);edge(name,'RefundPolicy','',kind='realize',s='t',t='b',via=[(x+122,335),(575,335)])
node('CancellationSchedule',40,600,w=310,fields=['id, version, windows: List'],methods=['+ official(), requireOfficial()','+ select(Duration)'])
node('HotelTime',420,600,w=310,fields=['ZONE: America/Bogota'],methods=['+ inHotelZone(Clock)'])
node('CopMoney',820,600,w=310,fields=['CURRENCY: COP','ROUNDING: HALF_UP'],methods=['+ positive(), integral()'])
edge('CancellationSchedule','RefundPolicy','selects 1 strategy',kind='dep',s='t',t='l',via=[(195,555),(385,555),(385,205)],pos=(220,547))
note(762,'Hotel uses the selected RefundPolicy inside bookingAtomic. Refunds preserve original payment/policy/version; helpers do not change official rates.')
P.page('4. Singleton, Factory Method, Builder y Template Method','Factory Method varia el producto. Template Method fija resolver tipo -> hook de construccion -> registrar. Son problemas distintos del mismo recorrido.')
node('RoomCreator',385,140,w=355,fields=[],methods=['+ create(Hotel, number, floor)','  resolve -> createRoom -> addRoom','- # createRoom(...), # category()'],kind='abstract')
for name,x in [('StandardRoomCreator',40),('FamilyRoomCreator',420),('SuiteRoomCreator',800)]:
 node(name,x,380,w=310,fields=[],methods=['# createRoom(...)','# category()']);edge(name,'RoomCreator','',kind='inherit',s='t',t='b',via=[(x+155,335),(560,335)])
for name,x in [('StandardRoom',40),('FamilyRoom',420),('SuiteRoom',800)]:node(name,x,600,w=310,fields=['Room subtype'],methods=['+ getFeatures()']);edge(['StandardRoomCreator','FamilyRoomCreator','SuiteRoomCreator'][[40,420,800].index(x)],name,'creates',kind='dep',s='b',t='t',pos=(x+175,563))
note(742,'All three room classes inherit Room. RoomCreatorRegistry aggregates creators by RoomCategory.', 'Singleton: Hotel.Holder owns default INSTANCE; getInstance() returns it. Tests/demo use separate controlled instances.', 'Builder: Reservation.Builder holds terms and build() validates/calculates before returning a Reservation (constructor private).')
P.page('5. Programa de puntos y Composite de paquetes','La cuenta es una proyeccion por cliente del PointsLedger privado de Hotel; no se agrega un segundo usuario ni una segunda tarjeta.')
node('PointsLedger',40,140,w=300,fields=['lots, redemptions, events','deferred sources, policy'],methods=['+ paid(), redeem(), refunded()','+ snapshotAccount(...)'])
node('PointsLedger.Lot',420,140,w=300,fields=['paymentId, clientId, earned','entitlement, used, expiry'],methods=[],kind='record')
node('PointsLedger.Redemption',820,140,w=300,fields=['reservationId, benefit, policy','allocations: List'],methods=[],kind='record')
edge('PointsLedger','PointsLedger.Lot','0..* owned lots',kind='compose',pos=(346,180));edge('PointsLedger.Lot','PointsLedger.Redemption','0..* allocations reference lot',s='r',t='l',pos=(730,180))
node('ServiceComponent',420,400,w=300,fields=[],methods=['+ services(), rawPrice()','+ price(), available()'],kind='interface')
node('ServiceComponent.Leaf',40,620,w=300,fields=['service: AdditionalService'],methods=['+ services()'],kind='record')
node('ServiceComponent.Package',420,620,w=300,fields=['id, name, description','children: List<ServiceComponent>'],methods=['+ services()'],kind='record')
node('Benefit',820,620,w=300,fields=['id, version, cost, kind','discount or targetId'],methods=[],kind='record')
edge('ServiceComponent.Leaf','ServiceComponent','',kind='realize',s='t',t='b',via=[(190,575),(570,575)]);edge('ServiceComponent.Package','ServiceComponent','',kind='realize',s='t',t='b');edge('ServiceComponent.Package','ServiceComponent','1..* children',kind='compose',s='r',t='r',via=[(775,687),(775,467)],pos=(784,570))
note(340,'Hotel owns one ledger; Client ID selects Account. A Lot references one original payment; a Redemption owns Allocation 1..*.', 'LoyaltyCatalog registers immutable packages and versioned benefits. Package construction rejects invalid/duplicate leaves.', 'LoyaltyPolicy records explicit accrual/refund decisions; no production defaults for rates or equivalences.')
P.page('6. Decorator y Specification (patron de dominio)','Decorator compone importes crudos en orden explicito. Specification no pertenece al catalogo GoF ni se cuenta como tercer comportamiento.')
node('PriceComponent',40,140,w=245,fields=[],methods=['+ amount(): BigDecimal'],kind='interface')
for name,x in [('PriceComponent.Base',325),('PriceComponent.Percent',610),('PriceComponent.Fixed',895)]:node(name,x,140,w=245,fields=['value or wrapped component'],methods=['+ amount()'],kind='record');edge(name,'PriceComponent','',kind='realize',s='b',t='b',via=[(x+120,325),(160,325)])
edge('PriceComponent.Percent','PriceComponent','1 wrapped',kind='aggregate',s='t',t='t',via=[(730,118),(160,118)],pos=(380,111))
node('PromotionDefinition',40,400,w=300,fields=['id, version, settings'],methods=['+ requireEligible(...)'],kind='record')
node('PromotionCondition',420,400,w=300,fields=[],methods=['+ satisfiedBy(Context)','+ and(...)'],kind='interface')
node('PromotionCondition.All',820,400,w=300,fields=['conditions: List'],methods=['+ satisfiedBy(...)'],kind='record')
edge('PromotionDefinition','PromotionCondition','1..* conditions',s='r',t='l',pos=(345,440));edge('PromotionCondition.All','PromotionCondition','',kind='realize',s='l',t='r');edge('PromotionCondition.All','PromotionCondition','1..* children',kind='compose',s='t',t='t',via=[(970,365),(570,365)],pos=(740,354))
note(610,'Other real Specifications: StayWindow, RoomTypes, MinimumNights, FrequentClient, Services and Packages implement PromotionCondition.', 'PromotionProgram stores immutable versions and Change history; validates current version against accepted Quote.', 'Fixed/Percent both wrap PriceComponent. No rounding during wrapping; CopMoney defines final positive integral obligation.', 'Hotel builds the trusted Context from registered data; UI cannot assert eligibility by sending a boolean.', 'Combination choices are explicit: EXCLUSIVE, PROMOTION_THEN_POINTS, POINTS_THEN_PROMOTION. No approved fixture implied.')
P.page('7. Reportes, Adapter y exportacion','El modelo de reporte no depende de JavaFX. El adaptador resuelve contratos incompatibles de datos de charts sin recalcular negocio.')
node('ReportService',40,140,w=260,fields=['hotel: Hotel'],methods=['+ generate(token, query)','+ export(...)'])
node('ReportEngine',390,140,w=260,fields=['one invocation per cut'],methods=['+ generate(snapshot,query)'])
node('ReportResult',820,140,w=300,fields=['sections, totals, charts','query, generatedAt, criteria'],methods=[],kind='record')
edge('ReportService','ReportEngine','creates/uses',kind='dep',pos=(309,185));edge('ReportEngine','ReportResult','produces',kind='dep',pos=(660,185))
node('ReportChartView',40,400,w=300,fields=[],methods=['+ render(ChartSpec, Type): Node'],kind='interface')
node('ReportChartAdapter',420,400,w=300,fields=[],methods=['+ render(...)'])
node('ReportScreen',820,400,w=300,fields=['chartView: ReportChartView','displayed: ReportResult'],methods=['+ show()'])
edge('ReportChartAdapter','ReportChartView','',kind='realize',s='l',t='r');edge('ReportScreen','ReportChartView','target port',kind='dep',s='b',t='b',via=[(970,575),(190,575)],pos=(465,567));edge('ReportChartAdapter','ReportResult','ChartSpec adaptee',kind='dep',s='t',t='b',via=[(570,337),(970,337)],pos=(706,328))
node('ReportExporter',40,650,w=300,fields=[],methods=['+ extension(), render(result)'],kind='interface')
node('TxtReportExporter',420,650,w=300,fields=['UTF-8 formatting'],methods=['+ render(result)'])
node('ReportFiles',820,650,w=300,fields=['temporary file -> destination'],methods=['+ write(...)'])
edge('TxtReportExporter','ReportExporter','',kind='realize',s='l',t='r')
note(610,'Adapter transforms ChartSpec into XYChart.Series/Data or PieChart.Data. ReportSnapshot is copied under Hotel monitor.', 'MoneyAllocation and PointsReportData are pure helpers. ReportExporter is extensible; TXT implemented, PDF export is future work.')
P.page('8. MVC, sesiones y proteccion de entidades','La vista usa DTOs y servicios; no llama setters de entidades. La proteccion sigue en el agregado, no solo en botones o navegacion.')
node('HotelApplication',40,140,w=260,fields=['shell: ShellController'],methods=['+ start(Stage), stop()'])
node('ShellController',390,140,w=300,fields=['DesktopController, FxActions','FXML root/content/navigation'],methods=['+ navigate(), logout()'])
node('DesktopController',820,140,w=300,fields=['token, user, screen, busy','service facades'],methods=['+ execute(), navigate()'])
edge('HotelApplication','ShellController','FXML loads',kind='dep',pos=(305,177));edge('ShellController','DesktopController','1 controller',pos=(705,177))
node('FxActions',40,400,w=260,fields=['one background executor','busy property'],methods=['+ run(...), close()'])
node('Hotel',390,400,w=300,fields=['sessions, authorization','monitor / bookingAtomic'],methods=['+ withSession(...)'])
node('ManagedEntity',820,400,w=300,fields=['guard: EntityGuard 0..1'],methods=['# change(...), readProtected()'],kind='abstract')
node('EntityGuard',820,650,w=300,fields=[],methods=['+ mutate(...), read(...)'],kind='interface')
edge('ManagedEntity','EntityGuard','0..1 installed guard',s='b',t='t',pos=(987,607));edge('DesktopController','Hotel','via facades',kind='dep',s='b',t='t',via=[(970,335),(540,335)],pos=(620,325));edge('ShellController','FxActions','1 execution adapter',s='b',t='t',via=[(540,335),(170,335)],pos=(180,325))
note(610,'Views: shell.fxml, login.fxml, hotel.css, Ui components. Controllers: LoginController and screen classes.', 'Screen classes: BookingScreen, ClientScreens, AdminScreens, CommercialScreens, ReportScreen. They dispatch facade requests.', 'HotelService, ReservationService, LoyaltyService, PromotionService, ReportService are Facade participants (one pattern).', 'User/Room/RoomType/Reservation/Promotion/AdditionalService/VirtualCard inherit ManagedEntity; Hotel installs its guard.')
P.page('9. Alcance, demostracion y trazabilidad','Diagrama complementario de la carga academica aislada y referencia para defender patrones; no inventa clases ni participacion.')
node('DemoDataFactory',40,140,w=300,fields=['Credentials (secrets redacted)','ReplayClock: private Clock'],methods=['+ create(credentials): Context'])
node('DemoFixtures',420,140,w=300,fields=['explicit academic values only'],methods=['+ rules(), points()'])
node('DemoSetupDialog',820,140,w=300,fields=['operator-supplied secrets','explicit fixture consent'],methods=['+ ask(): Optional'])
edge('DemoDataFactory','DemoFixtures','reads fixtures only',kind='dep',pos=(345,180));edge('DemoSetupDialog','DemoDataFactory','explicit --demo branch',kind='dep',s='b',t='b',via=[(970,325),(190,325)],pos=(470,314))
note(390,'DemoDataFactory creates a separate Hotel instance via package-scoped constructor; it never resets the normal Singleton.', 'ReplayClock records real domain operations on fixture dates and then delegates to live clock. No fake status setters.', 'Credentials are provided by operator, distinct, >=12 characters; never saved or logged. Normal startup has no seeds.', '', 'Pattern to page: Singleton / Factory Method / Builder / Template Method -> 4; State -> 2; Strategy -> 3;', 'Composite -> 5; Decorator -> 6; Adapter -> 7; Facade -> 8. Specification -> 6 (domain, not GoF).', '', 'Relationships intentionally show relevant domain/architectural dependencies, not every getter, enum or helper.', 'Full declaration inventory and source SHA-256: docs/uml/inventario-codigo.md. Source generator: docs/uml/generar_uml.py.', 'This provisional academic artifact is not the teachers official template. Team roles and external approvals remain pending.')
P.finish()
rows=['# Inventario real de declaraciones Java - H-06','', 'Generado desde src/main/java. Incluye clases/records/interfaces/enums anidados; no asigna autoria.', '', '| Archivo | Declaraciones | SHA-256 |','|---|---|---|']
for f in sorted(SRC.rglob('*.java')):
 names=re.findall(r'\b(?:class|interface|record|enum)\s+([A-Za-z_$][\w$]*)',f.read_text());rows.append('| '+str(f.relative_to(ROOT))+' | '+', '.join(dict.fromkeys(names))+' | '+hashlib.sha256(f.read_bytes()).hexdigest()+' |')
(Path(__file__).parent/'inventario-codigo.md').write_text('\n'.join(rows)+'\n')
print(OUT,OUT.stat().st_size,'bytes;',len(P.pages),'pages')
