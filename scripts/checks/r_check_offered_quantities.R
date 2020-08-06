
#to be run after a simulation run
#expected result: offered quantities moves the month after harvesting.

#### Choose a market session and extract offered quantities from simulation output

data.sessions<-read.csv("../../output/z_sessions.csv")
sessions<-levels(as.factor(data.sessions$SessionDescription))

print(sessions)
#choose a session
session.pos<-as.integer(readline(prompt="Enter a session number: "))
session<-sessions[session.pos]



#####offered quantities from simulation output
data.session<-data.sessions[which(data.sessions$SessionDescription==session),]

# transform simulation ticks in real time
str1<-system("grep startUsingInputsFromTimeTick= ../../src/cms_wheat/Cms_builder.java",intern=T)
str2<-unlist(strsplit(str1,"="))[2]
str3<-unlist(strsplit(str2,";"))[1]
start.real.data<-as.numeric(str3)

sim.ticks<-seq(1,length(data.session$OfferedQuantity))

food<-read.csv("../../data/buyers_Food.csv",)

first.year.of.demand<-as.numeric(unlist(strsplit(names(food)[5],"[.]"))[3])
first.year.of.production<-first.year.of.demand-1

time.sim.months<-first.year.of.demand-1/12-((sim.ticks)/12)[start.real.data]+sim.ticks/12+1/24

plot.from.pos<-which.min((time.sim.months-first.year.of.demand-1/24)^2)
plot.to.pos<-length(time.sim.months)

#####offered quantities from simulation input
data.producers.w<-read.csv("../../data/producers_w.csv")
data.producers.s<-read.csv("../../data/producers_s.csv")
producer<-unlist(strsplit(session," @ "))[1]
producer.pos<-which(data.producers.w$Area==producer)
data.producer.w<-data.producers.w[producer.pos,]
data.producer.s<-data.producers.s[producer.pos,]
productions<-data.producer.w[8:ncol(data.producers.w)]+data.producer.s[8:ncol(data.producers.s)]
nsessions<-length(unlist(strsplit(as.character(data.producer.w$markets),",")))

year.of.production<-first.year.of.production
month.of.production<-data.producer.w$GatherMonthW
monthly.supplies<-numeric()
year.vec<-numeric()
month.vec<-numeric()
for(tmp.prod in productions){
	for(tmp.month in 1:12){
		month.of.production<-month.of.production+1
		if(month.of.production>12){
			year.of.production<-year.of.production+1
			month.of.production<-1
		}
	monthly.supplies[length(monthly.supplies)+1]<-round(tmp.prod/(12*nsessions))
	year.vec[length(year.vec)+1]<-year.of.production
	month.vec[length(month.vec)+1]<-month.of.production
	}
}
year.month.vec<-year.vec+month.vec/12-1/24

#### plot

plot(time.sim.months[plot.from.pos:plot.to.pos],data.session$OfferedQuantity[plot.from.pos:plot.to.pos],type="l",xlab="time",ylab="offered quantity",main=session)
lines(year.month.vec,monthly.supplies,col=2)
lines(c(first.year.of.demand,first.year.of.demand),c(min(data.session$OfferedQuantity),max(data.session$OfferedQuantity)),lty=2)
lines(c(first.year.of.demand+1,first.year.of.demand+1),c(min(data.session$OfferedQuantity),max(data.session$OfferedQuantity)),lty=2)
lines(c(first.year.of.demand+2,first.year.of.demand+2),c(min(data.session$OfferedQuantity),max(data.session$OfferedQuantity)),lty=2)
lines(c(first.year.of.demand+3,first.year.of.demand+3),c(min(data.session$OfferedQuantity),max(data.session$OfferedQuantity)),lty=2)

#sim.reservationPrice<-data.producers[which(data.producers$Name=="United States of America"),9]


