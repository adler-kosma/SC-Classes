////Ska alla ha gate=1 så att jag kan stänga av synthen?
////Instansmetoder för synthar? Hur arbetar jag med syntharna då? ändrar parametrar
////Flera synthar under samma instansmetod? Hur kallar jag dem var och en för sig?

///////////////////////////////
//panBus -- skickar ljud till alla 4 högtalare genom \outSynth
//panBusC -- skickar ljud till \pannerCircle
//PanBusD -- skickar ljud till \discreteOut som spelas av en Pbindef

Konsert_studio {
	var server;
	var synth, <synthGroup, <fxGroup, <panGroup, <aBus, <bBus, <outBus, <revBus, <panBus, <panBusC, <panBusD, <midiBus, delBus, masterBus, masterSynth, gator_s, out_s, korg_s, moog_s, mic_s, klank_s, reverb_s, delay_s, panner_s;
	var fxAssignments;

	*new { //klassmetod
		^super.new.initKonsert;
	}

	initKonsert { //instansmetod
		server = Server.default;
		MIDIClient.init;
		midiBus = MIDIOut.new(1);
		CmdPeriod.add({(0..127).do{arg n; midiBus.noteOff(0,n)}});

		server.waitForBoot{
			Konsert.sendSynthDefs;
			server.sync;
			//~bdkrev = Buffer.readChannel(server,"/Users/adele21/Music/SuperCollider/klangwürfel-lead you home/lead_you_home_rev_rec_1.aiff", channels:[0]);
			~perc = Buffer.readChannel(server,"/Users/adele21/Documents/SuperCollider/Mobilen/ljuden/12-Wood_Block.wav", channels: [0]);
			//~kick = Buffer.readChannel(server,"/Users/adele21/Music/DRUMS/kicks från daniel/kick(mmm).wav", channels: [0]);
			aBus = Bus.audio(server, 1); // master
			bBus = Bus.audio(server, 1); // slave
			revBus = Bus.audio(server, 1); //reverb
			panBus = Bus.audio(server, 1); //panning to all channels
			panBusC = Bus.audio(server, 1); //panning circle
			panBusD = Bus.audio(server, 1); //panning discrete
			outBus = Bus.audio(server, 1); //routing passed gator through reverb
			delBus = Bus.audio(server, 1); //delay
			//masterBus  = Bus.audio(server, 4);
			synthGroup = Group.new(server);
			fxGroup = Group.after(synthGroup);
			panGroup = Group.after(fxGroup);
			//masterSynth = Synth.after(panGroup, \master);
			server.options.numOutputBusChannels = 16;
			server.options.numInputBusChannels = 4;
			server.options.sampleRate = 48000;
			server.recChannels = 16;

		};
	}

	gator {|parValEvent|
		fork{
			if(gator_s.isNil, {
				gator_s = Synth(\gator, nil, fxGroup, 'addToHead');
				server.sync;
				parValEvent.keysValuesDo{|par, val|
					gator_s.set(par, val);
				};
			}, {
				parValEvent.keysValuesDo{|par, val|
					gator_s.set(par, val);
				};
			});
		};
	}
	*sendSynthDefs {
	}
}